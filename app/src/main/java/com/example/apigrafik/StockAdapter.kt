package com.example.apigrafik

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apigrafik.Fragment.Stock
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class StockAdapter(
    private var stocks: List<Stock>,
    private val onItemClicked: (Stock) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    fun updateStocks(newStocks: List<Stock>) {
        stocks = newStocks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_saham, parent, false)
        return StockViewHolder(view, onItemClicked)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(stocks[position])
    }

    override fun getItemCount() = stocks.size

    class StockViewHolder(
        itemView: View,
        private val onItemClicked: (Stock) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val stockName: TextView = itemView.findViewById(R.id.stockName)
        private val stockSymbol: TextView = itemView.findViewById(R.id.stockSymbol)
        private val stockPrice: TextView = itemView.findViewById(R.id.stockPrice)
        private val stockChange: TextView = itemView.findViewById(R.id.stockChange)
        private val logoView: ImageView = itemView.findViewById(R.id.imageStock)
        private val chart: LineChart = itemView.findViewById(R.id.stockChart)

        fun bind(stock: Stock) {
            val stockBody = stock.body.firstOrNull() ?: return

            stockName.text = stockBody.shortName
            stockSymbol.text = stockBody.symbol

            val isRupiah = stockBody.symbol.endsWith(".JK")
            val currencySymbol = if (isRupiah) "Rp. " else "$ "
            stockPrice.text = String.format("%s%.2f", currencySymbol, stockBody.regularMarketPrice)

            stockChange.text = String.format("%.2f%%", stockBody.regularMarketChangePercent)
            stockChange.setTextColor(
                if (stockBody.regularMarketChangePercent >= 0) Color.GREEN else Color.RED
            )

            val logoMap = mapOf(
                "AAPL" to R.drawable.logo_apple,
                "GOOGL" to R.drawable.logo_google,
                "MSFT" to R.drawable.logo_msft,
                "AMZN" to R.drawable.logo_amazon,
                "NVDA" to R.drawable.logo_nvidia,
                "TSLA" to R.drawable.logo_tesla,
                "META" to R.drawable.logo_meta,
                "BRK-A" to R.drawable.logo_brk_h,
                "JNJ" to R.drawable.logo_jnj,
                "V" to R.drawable.logo_v,
                "BABA" to R.drawable.logo_alibaba,
                "NFLX" to R.drawable.logo_netflix,
                "DIS" to R.drawable.logo_disney,
                "PYPL" to R.drawable.logo_paypal,
                "INTC" to R.drawable.logo_intel,
                "CSCO" to R.drawable.logo_cisco,
                "AMD" to R.drawable.logo_amd,
                "WMT" to R.drawable.logo_walmart,
                "KO" to R.drawable.logo_cocacola,
                "PFE" to R.drawable.logo_pfizer,
            )
            logoView.setImageResource(logoMap[stockBody.symbol] ?: R.drawable.ic_placeholder)

            setupChart(chart, stock)

            itemView.setOnClickListener {
                onItemClicked(stock)
            }
        }

        private fun setupChart(chart: LineChart, stock: Stock) {
            val entries = listOf(
                Entry(0f, stock.body.first().fiftyTwoWeekLow.toFloat()),
                Entry(1f, stock.body.first().regularMarketPrice.toFloat()),
                Entry(2f, stock.body.first().fiftyTwoWeekHigh.toFloat())
            )

            val dataSet = LineDataSet(entries, "Price Movement").apply {
                color = if (stock.body.first().regularMarketChangePercent >= 0) Color.GREEN else Color.RED
                setDrawCircles(false)
                setDrawFilled(false)
                lineWidth = 1.5f
            }

            chart.setDrawGridBackground(false)
            chart.xAxis.setDrawGridLines(false)
            chart.axisLeft.setDrawGridLines(false)
            chart.axisRight.setDrawGridLines(false)
            chart.legend.isEnabled = false
            chart.setTouchEnabled(false)
            chart.setMarker(null)
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    }
}
