import os
import pickle
import numpy as np
import pandas as pd
from sklearn.preprocessing import MinMaxScaler
from tensorflow.keras.models import Sequential, load_model
from tensorflow.keras.layers import LSTM, Dense
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
from flask import Flask, request, jsonify

class StockInvestmentRecommender:
    def __init__(self):
        self.models = {}
        self.scalers = {}
        self.stock_data = None
        self.model_dir = 'saved_models'
        self.data_file = 'stock_data.pkl'
        self.accuracy_metrics = {}

    def prepare_data(self, data, sequence_length=60):
        """Prepare data untuk LSTM"""
        scaler = MinMaxScaler()
        scaled_data = scaler.fit_transform(data.reshape(-1, 1))

        X, y = [], []
        for i in range(sequence_length, len(scaled_data)):
            X.append(scaled_data[i-sequence_length:i, 0])
            y.append(scaled_data[i, 0])

        return np.array(X), np.array(y), scaler

    def create_lstm_model(self, sequence_length):
        """Membuat model LSTM"""
        model = Sequential([
            LSTM(50, return_sequences=True, input_shape=(sequence_length, 1)),
            LSTM(50, return_sequences=False),
            Dense(25),
            Dense(1)
        ])
        model.compile(optimizer='adam', loss='mean_squared_error')
        return model

    def train_models(self, df, sequence_length=60):
        """Training model untuk setiap saham"""
        self.stock_data = df
        print(f"Training models for {len(df['code'].unique())} stocks...")

        for code in df['code'].unique():
            print(f"Training model for {code}")
            stock_df = df[df['code'] == code]
            close_prices = stock_df['close'].values

            X, y, scaler = self.prepare_data(close_prices, sequence_length)
            X = X.reshape((X.shape[0], X.shape[1], 1))

            model = self.create_lstm_model(sequence_length)
            model.fit(X, y, epochs=50, batch_size=32, validation_split=0.2, verbose=0)

            self.models[code] = model
            self.scalers[code] = scaler

            # Evaluasi akurasi setelah training
            X_val = X[-int(len(X) * 0.2):]
            y_val = y[-int(len(y) * 0.2):]
            self.evaluate_model_accuracy(code, X_val, y_val)

        print("Training completed!")

    def predict_future_prices(self, code, days=365):
        """Memprediksi harga saham"""
        if code not in self.models:
            raise ValueError(f"No model trained for stock {code}")

        stock_df = self.stock_data[self.stock_data['code'] == code]
        last_60_days = stock_df['close'].values[-60:]

        scaler = self.scalers[code]
        scaled_data = scaler.transform(last_60_days.reshape(-1, 1))

        predictions = []
        current_batch = scaled_data.reshape((1, 60, 1))

        for _ in range(days):
            pred = self.models[code].predict(current_batch, verbose=0)
            predictions.append(pred[0, 0])
            current_batch = np.roll(current_batch, -1)
            current_batch[0, -1, 0] = pred[0, 0]

        predictions = scaler.inverse_transform(np.array(predictions).reshape(-1, 1))
        return predictions.flatten()

    def calculate_metrics(self, code, investment_years, expected_return):
        """Menghitung metrik investasi"""
        days = investment_years * 365
        future_prices = self.predict_future_prices(code, days)

        initial_price = self.stock_data[self.stock_data['code'] == code]['close'].iloc[-1]
        final_price = future_prices[-1]

        annual_return = ((final_price / initial_price) ** (1 / investment_years)) - 1
        returns = np.diff(future_prices) / future_prices[:-1]
        volatility = np.std(returns) * np.sqrt(252)

        return {
            'code': code,
            'predicted_annual_return': annual_return,
            'volatility': volatility,
            'meets_return_target': annual_return >= expected_return
        }

    def recommend_stocks(self, investment_amount, investment_years, expected_return):
        """Memberikan rekomendasi saham"""
        recommendations = []

        for code in self.models.keys():
            metrics = self.calculate_metrics(code, investment_years, expected_return)
            current_price = self.stock_data[self.stock_data['code'] == code]['close'].iloc[-1]
            possible_shares = investment_amount // current_price

            metrics.update({
                'current_price': current_price,
                'possible_shares': possible_shares,
                'required_investment': possible_shares * current_price
            })

            recommendations.append(metrics)

        valid_recommendations = [r for r in recommendations if r['meets_return_target']]
        sorted_recommendations = sorted(
            valid_recommendations,
            key=lambda x: x['predicted_annual_return'] / x['volatility'],
            reverse=True
        )

        return sorted_recommendations

    def save_models(self):
        """Menyimpan model dan scaler"""
        if not os.path.exists(self.model_dir):
            os.makedirs(self.model_dir)

        with open(os.path.join(self.model_dir, self.data_file), 'wb') as f:
            pickle.dump(self.stock_data, f)

        for code in self.models:
            model_path = os.path.join(self.model_dir, f'model_{code}.h5')
            self.models[code].save(model_path)

            scaler_path = os.path.join(self.model_dir, f'scaler_{code}.pkl')
            with open(scaler_path, 'wb') as f:
                pickle.dump(self.scalers[code], f)

    def load_models(self):
        """Memuat model dan scaler yang telah disimpan"""
        with open(os.path.join(self.model_dir, self.data_file), 'rb') as f:
            self.stock_data = pickle.load(f)

        for file in os.listdir(self.model_dir):
            if file.endswith('.h5'):
                stock_code = file.split('_')[1].replace('.h5', '')
                self.models[stock_code] = load_model(os.path.join(self.model_dir, file))

                scaler_path = os.path.join(self.model_dir, f'scaler_{stock_code}.pkl')
                with open(scaler_path, 'rb') as f:
                    self.scalers[stock_code] = pickle.load(f)

    def evaluate_model_accuracy(self, code, X_val, y_val):
        """Evaluasi model LSTM"""
        y_pred = self.models[code].predict(X_val, verbose=0)
        mse = mean_squared_error(y_val, y_pred)
        mae = mean_absolute_error(y_val, y_pred)
        r2 = r2_score(y_val, y_pred)

        self.accuracy_metrics[code] = {'mse': mse, 'mae': mae, 'r2': r2}

app = Flask(__name__)
recommender = StockInvestmentRecommender()

@app.route('/recommend', methods=['POST'])
def recommend():
    data = request.json
    investment_amount = data.get('investment_amount', 10000000)
    investment_years = data.get('investment_years', 1)
    expected_return = data.get('expected_return', 0.1)
    recommendations = recommender.recommend_stocks(investment_amount, investment_years, expected_return)
    return jsonify({"status": "success", "recommendations": recommendations})


if __name__ == '__main__':
    recommender.load_models()
    app.run(debug=True, port=5000)