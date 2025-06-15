package com.example.apigrafik.Fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.apigrafik.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.NumberFormat
import java.util.Locale

class DetailFragment : Fragment() {

    private var isFavorite = false
    private val db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private var stockSymbolText: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.detail, container, false)

        auth = FirebaseAuth.getInstance()
        stockSymbolText = arguments?.getString("stockSymbol") ?: ""

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

        // Only check favorites if we have a valid symbol
        if (stockSymbolText.isNotEmpty()) {
            checkIfFavorite(favoriteButton)
        }

        favoriteButton.setOnClickListener {
            toggleFavorite(favoriteButton)
        }

        arguments?.let { args ->
            // Format currency values
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

            // Set basic info
            stockName.text = args.getString("stockName", "Unknown")
            stockSymbol.text = args.getString("stockSymbol", "Unknown")

            // Determine the currency symbol
            val stockSymbolText = args.getString("stockSymbol", "")
            val isRupiah = stockSymbolText.endsWith(".JK")
            val currencySymbol = if (isRupiah) "Rp. " else "$ "

            // Format price with the correct currency symbol
            val priceValue = args.getFloat("stockPriceFloat", 0f)
            stockPrice.text = "$currencySymbol${currencyFormat.format(priceValue).replace(currencyFormat.currency.symbol, "").trim()}"

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

            // Format and set daily range
            val highValue = args.getFloat("stockDayHigh", 0f)
            val lowValue = args.getFloat("stockDayLow", 0f)
            stockDayRange.text = "High: $currencySymbol${currencyFormat.format(highValue).replace(currencyFormat.currency.symbol, "").trim()}\n" +
                    "Low: $currencySymbol${currencyFormat.format(lowValue).replace(currencyFormat.currency.symbol, "").trim()}"

            // Format market cap with suffix (B/T)
            val marketCap = args.getString("stockMarketCap", "Unknown")
            stockMarketCap.text = if (marketCap != "Unknown") {
                "$currencySymbol$marketCap"
            } else {
                "Unknown"
            }

            // Format volume with suffix (M/B)
            stockVolume.text = args.getString("stockVolume", "Unknown")

            // Format dividend yield
            stockDividendYield.text = "${args.getString("stockDividendYield", "0")}%"
            fullExchangeName.text = args.getString("fullExchangeName", "Unknown")
            analystrating.text = args.getString("averageAnalystRating", "Unknown")

            // Set dividend date
            val dividendDate = arguments?.getString("dividendDate", "N/A")
            val tvDividendDate = view.findViewById<TextView>(R.id.tvdividenDate)
            tvDividendDate.text = dividendDate

            // Setup chart
            setupEnhancedChart(
                stockChart,
                args.getFloat("stockLow", 0f),
                args.getFloat("stockHigh", 0f),
                args.getFloat("stockPriceFloat", 0f),
                args.getString("stockChange", "0")
            )
        }

        // Setup click listeners
        backIcon.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        return view
    }

    private fun checkIfFavorite(favoriteButton: FloatingActionButton) {
        val currentUser = auth.currentUser
        if (currentUser != null && stockSymbolText.isNotEmpty()) {
            db.collection("users")
                .document(currentUser.uid)
                .collection("favorites")
                .document(stockSymbolText)
                .get()
                .addOnSuccessListener { document ->
                    isFavorite = document.exists()
                    updateFavoriteButton(favoriteButton)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Error checking favorite status: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun toggleFavorite(favoriteButton: FloatingActionButton) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please login to add favorites", Toast.LENGTH_SHORT).show()
            return
        }

        if (stockSymbolText.isEmpty()) {
            Toast.makeText(context, "Invalid stock symbol", Toast.LENGTH_SHORT).show()
            return
        }

        val favoriteRef = db.collection("users")
            .document(currentUser.uid)
            .collection("favorites")
            .document(stockSymbolText)

        if (isFavorite) {
            favoriteRef.delete()
                .addOnSuccessListener {
                    isFavorite = false
                    updateFavoriteButton(favoriteButton)
                    Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error removing favorite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            val favoriteData = hashMapOf(
                "symbol" to stockSymbolText,
                "timestamp" to System.currentTimeMillis()
            )

            favoriteRef.set(favoriteData)
                .addOnSuccessListener {
                    isFavorite = true
                    updateFavoriteButton(favoriteButton)
                    Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error adding favorite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateFavoriteButton(favoriteButton: FloatingActionButton) {
        favoriteButton.setImageResource(if (isFavorite) R.drawable.fav_aktif else R.drawable.fav_pasif)
        favoriteButton.backgroundTintList = if (isFavorite) {
            ColorStateList.valueOf(Color.RED)
        } else {
            ColorStateList.valueOf(Color.GRAY)
        }
    }

    private fun setupEnhancedChart(chart: LineChart, fiftyTwoWeekLow: Float, regularMarketPrice: Float, fiftyTwoWeekHigh: Float, stockChange: String) {
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

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(true)
            textColor = Color.WHITE
            textSize = 12f
            setLabelCount(3, true)
        }

        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.parseColor("#20FFFFFF")
            textColor = Color.WHITE
            textSize = 12f
            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        }
        chart.axisRight.isEnabled = false

        val chartColor = if (stockChange.startsWith("+")) {
            Color.parseColor("#4CAF50")
        } else {
            Color.parseColor("#F44336")
        }

        val entries = listOf(
            Entry(0f, fiftyTwoWeekLow),
            Entry(1f, regularMarketPrice),
            Entry(2f, fiftyTwoWeekHigh)
        )

        val dataSet = LineDataSet(entries, "52-Week Price Performance").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            color = chartColor
            lineWidth = 2.5f
            setDrawCircles(true)
            setCircleColor(chartColor)
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleRadius = 2f
            setDrawFilled(true)
            fillAlpha = 50
            fillColor = chartColor
            highLightColor = Color.WHITE
            setDrawHorizontalHighlightIndicator(false)
        }

        chart.data = LineData(dataSet)
        chart.animateX(1000)
        chart.invalidate()
    }

    companion object {
        fun newInstance(args: Bundle): DetailFragment {
            val fragment = DetailFragment()
            fragment.arguments = args
            return fragment
        }
    }
}