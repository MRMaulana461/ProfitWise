package com.example.apigrafik.Fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.apigrafik.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

class DetailFragment : Fragment() {

    private var isFavorite = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.detail, container, false)

        // Initialize views
        val stockName = view.findViewById<TextView>(R.id.tvLongName)
        val stockSymbol = view.findViewById<TextView>(R.id.tvSymbol)
        val stockPrice = view.findViewById<TextView>(R.id.tvRegularMarketPrice)
        val stockChange = view.findViewById<TextView>(R.id.tvRegularMarketChange)
        val stockDayRange = view.findViewById<TextView>(R.id.tvDayRange)
        val stockMarketCap = view.findViewById<TextView>(R.id.tvMarketCap)
        val stockVolume = view.findViewById<TextView>(R.id.tvVolume)
        val stockDividendYield = view.findViewById<TextView>(R.id.tvDividendYield)
        val fullExchangeName = view.findViewById<TextView>(R.id.tvfullExchangeName)
        val stockChart = view.findViewById<LineChart>(R.id.stockChart2)
        val backIcon = view.findViewById<ImageButton>(R.id.backIcon)
        val favoriteButton = view.findViewById<FloatingActionButton>(R.id.favoriteIcon)
        val analystrating = view.findViewById<TextView>(R.id.tvAnalystRating)

        arguments?.let { args ->
            // Format currency values
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

            // Set basic info
            stockName.text = args.getString("stockName", "Unknown")
            stockSymbol.text = args.getString("stockSymbol", "Unknown")

            // Format price with currency
            val priceValue = args.getFloat("stockPriceFloat", 0f)
            stockPrice.text = currencyFormat.format(priceValue)

            // Format and set price change with color
            val changeValue = args.getString("stockChange", "0")
            stockChange.apply {
                text = changeValue
                setTextColor(if (changeValue.startsWith("+")) {
                    Color.parseColor("#4CAF50") // Green for positive
                } else {
                    Color.parseColor("#F44336") // Red for negative
                })
            }

            // Format ranges
            stockDayRange.text = "High: ${currencyFormat.format(args.getFloat("stockDayHigh", 0f))}\nLow: ${currencyFormat.format(args.getFloat("stockDayLow", 0f))}"

            // Format market cap with suffix (B/T)
            val marketCap = args.getString("stockMarketCap", "Unknown")
            stockMarketCap.text = marketCap

            // Format volume with suffix (M/B)
            stockVolume.text = args.getString("stockVolume", "Unknown")

            // Format dividend yield
            stockDividendYield.text = "${args.getString("stockDividendYield", "0")}%"

            fullExchangeName.text = args.getString("fullExchangeName", "Unknown")

            analystrating.text = args.getString("averageAnalystRating", "Unknown")

            val dividendDate = arguments?.getString("dividendDate", "N/A")
            val tvDividendDate = view.findViewById<TextView>(R.id.tvdividenDate)
            tvDividendDate.text = dividendDate

            // Setup chart
            setupEnhancedChart(
                stockChart,
                args.getFloat("stockLow", 0f),
                args.getFloat("stockHigh", 0f),
                args.getFloat("stockPriceFloat", 0f),
                args.getString("stockChange", "0")  // Kirimkan stockChange ke fungsi
            )
        }

        // Setup click listeners
        backIcon.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        favoriteButton.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteButton(favoriteButton)
        }

        return view
    }

    private fun setupEnhancedChart(chart: LineChart, fiftyTwoWeekLow: Float, regularMarketPrice: Float, fiftyTwoWeekHigh: Float, stockChange: String) {
        // Chart style configuration
        chart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.isEnabled = false
            setViewPortOffsets(60f, 20f, 60f, 40f)
        }

        // X-Axis configuration
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(true)
            textColor = Color.WHITE
            textSize = 12f
            setLabelCount(3, true)
        }

        // Y-Axis configuration
        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.parseColor("#20FFFFFF")
            textColor = Color.WHITE
            textSize = 12f
            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        }
        chart.axisRight.isEnabled = false

        // Determine the color based on stock change
        val chartColor = if (stockChange.startsWith("+")) {
            Color.parseColor("#4CAF50") // Green for positive change
        } else {
            Color.parseColor("#F44336") // Red for negative change
        }

        // Create data points for 52-week performance
        val entries = listOf(
            Entry(0f, fiftyTwoWeekLow),  // Titik pertama: 52-week low
            Entry(1f, regularMarketPrice),  // Titik kedua: harga pasar sekarang
            Entry(2f, fiftyTwoWeekHigh)   // Titik ketiga: 52-week high
        )

        // Create and style the dataset
        val dataSet = LineDataSet(entries, "52-Week Price Performance").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            color = chartColor  // Menggunakan warna berdasarkan perubahan harga
            lineWidth = 2.5f
            setDrawCircles(true)
            setCircleColor(chartColor)
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleRadius = 2f
            setDrawFilled(true)
            fillAlpha = 50
            fillColor = chartColor  // Mengisi warna grafik dengan warna perubahan harga
            highLightColor = Color.WHITE
            setDrawHorizontalHighlightIndicator(false)
        }

        // Set data to chart
        chart.data = LineData(dataSet)
        chart.animateX(1000)
        chart.invalidate()
    }

    private fun updateFavoriteButton(favoriteButton: FloatingActionButton) {
        val iconResource = if (isFavorite) {
            R.drawable.fav_aktif
        } else {
            R.drawable.fav_pasif
        }
        favoriteButton.setImageResource(iconResource)
    }

    companion object {
        fun newInstance(args: Bundle): DetailFragment {
            val fragment = DetailFragment()
            fragment.arguments = args
            return fragment
        }
    }
}