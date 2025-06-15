package com.example.apigrafik.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.apigrafik.R
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// Updated data classes to match Flask API response
data class InvestmentRecommendation(
    val recommendations: List<Recommendation>,
    val parameters: InputParameters
)

data class InputParameters(
    val investment_amount: Double,
    val investment_days: Int,
    val expected_return: Double
)

data class Recommendation(
    val code: String,
    val current_price: Double,
    val possible_shares: Int,
    val required_investment: Double,
    val average_daily_return: Double,
    val volatility: Double,
    val total_return: Double,
    val meets_return_target: Int,
    val predicted_prices: List<Double>
)

data class InvestmentRequest(
    val investment_amount: Double,
    val investment_days: Int,
    val expected_return: Double
)

interface ApiService {
    @POST("recommend")  // Updated to match Flask endpoint
    @Headers("Content-Type: application/json")
    fun recommend(@Body request: InvestmentRequest): Call<InvestmentRecommendation>
}

class ChartFragment : Fragment() {
    private lateinit var totalInvestasiEditText: EditText
    private lateinit var returnInvestasiEditText: EditText
    private lateinit var jangkaWaktuEditText: EditText
    private lateinit var prediksiButton: Button
    private lateinit var resultTextView: TextView

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val TAG = "ChartFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.prediksi, container, false)

        totalInvestasiEditText = view.findViewById(R.id.totalInvestasi)
        returnInvestasiEditText = view.findViewById(R.id.returnInvestasi)
        jangkaWaktuEditText = view.findViewById(R.id.jangkaWaktu)
        prediksiButton = view.findViewById(R.id.prediksiButton)
        resultTextView = view.findViewById(R.id.resultTextView)

        // Membuat OkHttpClient dengan pengaturan timeout
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)   // Set connect timeout
            .readTimeout(30, TimeUnit.SECONDS)      // Set read timeout
            .writeTimeout(30, TimeUnit.SECONDS)     // Set write timeout
            .build()

        // Membuat Retrofit dengan OkHttpClient yang sudah dikonfigurasi
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.43.244:5000/") // Gantilah dengan base URL Anda
            .client(okHttpClient)  // Menambahkan OkHttpClient ke Retrofit
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        prediksiButton.setOnClickListener {
            val request = validateInput()
            if (request != null) {
                fetchRecommendations(apiService, request)
            }
        }

        return view
    }

    private fun validateInput(): InvestmentRequest? {
        val totalInvestasi = totalInvestasiEditText.text.toString().toDoubleOrNull()
        val returnInvestasi = returnInvestasiEditText.text.toString().toDoubleOrNull()?.div(100)
        val jangkaWaktu = jangkaWaktuEditText.text.toString().toIntOrNull()

        return when {
            totalInvestasi == null || returnInvestasi == null || jangkaWaktu == null -> {
                showToast("Semua kolom harus diisi dengan benar")
                null
            }
            totalInvestasi < 100_000 -> {
                showToast("Investasi minimal Rp 100.000")
                null
            }
            jangkaWaktu !in 1..365 -> {
                showToast("Jangka waktu harus antara 1-365 hari")
                null
            }
            returnInvestasi !in 0.0..1.0 -> {
                showToast("Return harus antara 0% - 100%")
                null
            }
            else -> InvestmentRequest(
                investment_amount = totalInvestasi,
                investment_days = jangkaWaktu,
                expected_return = returnInvestasi
            )
        }
    }

    private fun fetchRecommendations(apiService: ApiService, request: InvestmentRequest) {
        resultTextView.text = "Memuat rekomendasi..."
        prediksiButton.isEnabled = false

        Log.d(TAG, "Request: ${Gson().toJson(request)}")

        apiService.recommend(request).enqueue(object : Callback<InvestmentRecommendation> {
            override fun onResponse(
                call: Call<InvestmentRecommendation>,
                response: Response<InvestmentRecommendation>
            ) {
                prediksiButton.isEnabled = true
                if (response.isSuccessful) {
                    response.body()?.let { displayRecommendations(it.recommendations) }
                } else {
                    val error = response.errorBody()?.string()
                    resultTextView.text = "Gagal mendapatkan rekomendasi (${response.code()}): $error"
                    Log.e(TAG, "Error Response: $error")
                }
            }

            override fun onFailure(call: Call<InvestmentRecommendation>, t: Throwable) {
                prediksiButton.isEnabled = true
                resultTextView.text = "Gagal terhubung ke server: ${t.message}"
                Log.e(TAG, "Network Error", t)
            }
        })
    }

    private fun displayRecommendations(recommendations: List<Recommendation>) {
        if (recommendations.isNotEmpty()) {
            val result = recommendations.joinToString(separator = "\n\n") { rec ->
                """
            Kode Saham: ${rec.code}
            Harga Saat Ini: ${currencyFormat.format(rec.current_price)}
            Jumlah Saham: ${rec.possible_shares} lembar
            Total Investasi: ${currencyFormat.format(rec.required_investment)}
            Return Harian Rata-rata: ${String.format("%.2f", rec.average_daily_return * 100)}%
            Volatilitas: ${String.format("%.2f", rec.volatility * 100)}%
            Estimasi Total Return: ${String.format("%.2f", rec.total_return * 100)}%
            Status Target Return: ${if (rec.meets_return_target == 1) "Memenuhi" else "Tidak Memenuhi"}
            """.trimIndent()
            }
            resultTextView.text = result
        } else {
            resultTextView.text = "Tidak ada rekomendasi yang memenuhi kriteria Anda."
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}