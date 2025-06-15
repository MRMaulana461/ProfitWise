package com.example.apigrafik.Fragment

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.apigrafik.R
import com.example.apigrafik.StockViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomnavigation.BottomNavigationView

class SearchFragment : Fragment() {
    private lateinit var searchEditText: EditText
    private lateinit var searchResultsLayout: LinearLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var viewModel: StockViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[StockViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.search, container, false)

        searchEditText = view.findViewById(R.id.searchEditText)
        searchResultsLayout = view.findViewById(R.id.searchResultsLayout)
        bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation)

        setupUI()
        setupObservers()

        return view
    }

    private fun setupUI() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                val query = editable.toString()
                performSearch(query)
            }
        })
    }

    private fun setupObservers() {
        // Load data if not already loaded
        viewModel.loadStockData()
    }

    private fun performSearch(query: String) {
        val stocks = viewModel.stocks.value ?: return

        val searchResults = if (query.isEmpty()) {
            emptyList()
        } else {
            stocks.filter { stock ->
                val stockBody = stock.body.firstOrNull()
                stockBody?.let {
                    it.symbol.contains(query, ignoreCase = true) ||
                            it.longName.contains(query, ignoreCase = true) ||
                            it.shortName.contains(query, ignoreCase = true)  // Added shortName to search criteria
                } ?: false
            }
        }

        displaySearchResults(searchResults)
    }

    private fun displaySearchResults(stocks: List<Stock>) {
        searchResultsLayout.removeAllViews()

        for (stock in stocks) {
            val stockView = layoutInflater.inflate(R.layout.item_list_saham, null)
            val stockBody = stock.body.firstOrNull() ?: continue

            // Set stock name and symbol
            stockView.findViewById<TextView>(R.id.stockName).text = stockBody.longName  // Changed to shortName for display
            stockView.findViewById<TextView>(R.id.stockSymbol).text = stockBody.symbol

            // Set price with currency symbol
            val isRupiah = stockBody.symbol.endsWith(".JK")
            val currencySymbol = if (isRupiah) "Rp. " else "$ "
            val formattedPrice = String.format("%s%.2f", currencySymbol, stockBody.regularMarketPrice)
            stockView.findViewById<TextView>(R.id.stockPrice).text = formattedPrice

            // Set price change with color
            val changeView = stockView.findViewById<TextView>(R.id.stockChange)
            changeView.text = String.format("%.2f%%", stockBody.regularMarketChangePercent)
            changeView.setTextColor(
                if (stockBody.regularMarketChangePercent >= 0) Color.GREEN else Color.RED
            )

            val logoView = stockView.findViewById<ImageView>(R.id.imageStock)
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
                "PFE" to R.drawable.logo_pfe,
                "BBCA.JK" to R.drawable.logo_bbca,
                "BBNI.JK" to R.drawable.logo_bbni,
                "BBRI.JK" to R.drawable.logo_bbri,
                "GOTO.JK" to R.drawable.logo_goto,
                "TLKM.JK" to R.drawable.logo_tlkm,
                "UNVR.JK" to R.drawable.logo_unvr,
                "SIDO.JK" to R.drawable.logo_sido,
                "EMTK.JK" to R.drawable.logo_emtk,
                "ADRO.JK" to R.drawable.logo_adro,
                "ANTM.JK" to R.drawable.logo_antm
            )
            logoView.setImageResource(logoMap[stockBody.symbol] ?: R.drawable.ic_placeholder)

            val chart = stockView.findViewById<LineChart>(R.id.stockChart)
            setupChart(chart, stock)

            // Adding event listener to navigate to detail page
            stockView.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("stockName", stockBody.longName)
                    putString("stockSymbol", stockBody.symbol)
                    putFloat("stockPriceFloat", stockBody.regularMarketPrice.toFloat())

                    // Format price change percentage
                    val changeSymbol = if (stockBody.regularMarketChangePercent >= 0) "+" else ""
                    putString("stockChange", "$changeSymbol${String.format("%.2f%%", stockBody.regularMarketChangePercent)}")

                    // Send raw values for ranges
                    putFloat("stockDayLow", stockBody.regularMarketDayLow.toFloat())
                    putFloat("stockDayHigh", stockBody.regularMarketDayHigh.toFloat())

                    // Format market cap
                    putString("stockMarketCap", stockBody.marketCap?.let { formatMarketCap(it) } ?: "N/A")

                    // Format volume
                    putString("stockVolume", formatVolume(stockBody.regularMarketVolume))

                    // Format dividend yield
                    putString("stockDividendYield", stockBody.dividendYield?.let { String.format("%.2f", it) } ?: "N/A")

                    putString("fullExchangeName", stockBody.fullExchangeName)

                    // Send raw values for 52 week range
                    putFloat("stockLow", stockBody.fiftyTwoWeekLow.toFloat())
                    putFloat("stockHigh", stockBody.fiftyTwoWeekHigh.toFloat())
                }

                // Navigating to DetailFragment with the bundle
                val detailFragment = DetailFragment()
                detailFragment.arguments = bundle
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            }

            searchResultsLayout.addView(stockView)
        }
    }

    // Helper function to format volume
    private fun formatVolume(volume: Long): String {
        return when {
            volume >= 1_000_000_000 -> String.format("%.1fB", volume / 1_000_000_000.0)
            volume >= 1_000_000 -> String.format("%.1fM", volume / 1_000_000.0)
            volume >= 1_000 -> String.format("%.1fK", volume / 1_000.0)
            else -> volume.toString()
        }
    }

    // Fungsi untuk format Market Cap
    fun formatMarketCap(marketCap: Long): String {
        return when {
            marketCap >= 1_000_000_000_000 -> String.format("%.1fT", marketCap / 1_000_000_000_000.0) // Trillions
            marketCap >= 1_000_000_000 -> String.format("%.1fB", marketCap / 1_000_000_000.0) // Billions
            marketCap >= 1_000_000 -> String.format("%.1fM", marketCap / 1_000_000.0) // Millions
            marketCap >= 1_000 -> String.format("%.1fK", marketCap / 1_000.0) // Thousands
            else -> marketCap.toString()
        }
    }

    private fun setupChart(chart: LineChart, stock: Stock) {
        chart.setDrawGridBackground(false)
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawGridLines(false)
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)
        chart.setMarker(null)

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

        chart.data = LineData(dataSet)
        chart.invalidate()
    }
}