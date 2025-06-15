package com.example.apigrafik.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apigrafik.R
import com.example.apigrafik.StockAdapter
import com.example.apigrafik.StockViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListSahamFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var viewModel: StockViewModel
    private var currentCategory = "All Stocks" // Tambahkan variable untuk menyimpan kategori

    private val favoriteSymbols = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list_saham, container, false)

        // Restore kategori yang tersimpan jika ada
        savedInstanceState?.getString("currentCategory")?.let {
            currentCategory = it
        }

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = StockAdapter(emptyList()) { stock ->
            navigateToDetail(stock)
        }
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[StockViewModel::class.java]
        setupObservers()

        val btnCategory = view.findViewById<LinearLayout>(R.id.category)
        btnCategory.setOnClickListener {
            showCategoryBottomSheet()
        }

        // Ambil data favorit dari Firestore
        fetchFavoritesFromFirestore()

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentCategory", currentCategory)
    }

    private fun fetchFavoritesFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val docRef = firestore.collection("users").document(userId).collection("favorites")

        docRef.get()
            .addOnSuccessListener { querySnapshot ->
                favoriteSymbols.clear()
                querySnapshot.documents.forEach { document ->
                    val symbol = document.id
                    favoriteSymbols.add(symbol)
                }
                // Tampilkan kategori yang sedang aktif setelah mendapatkan data favorit
                displayFilteredStocks(currentCategory)
                updateCategoryView(currentCategory)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to fetch favorites: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCategoryBottomSheet() {
        val bottomSheet = CategoryBottomSheetFragment()
        bottomSheet.setOnCategorySelectedListener { category ->
            currentCategory = category // Update kategori saat ini
            updateCategoryView(category)
            displayFilteredStocks(category)
        }
        bottomSheet.show(parentFragmentManager, bottomSheet.tag)
    }

    private fun setupObservers() {
        viewModel.stocks.observe(viewLifecycleOwner) { stocks ->
            adapter.updateStocks(stocks)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
        }

        viewModel.loadStockData()

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
            progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun navigateToDetail(stock: Stock) {
        val stockBody = stock.body.firstOrNull() ?: return
        val bundle = Bundle().apply {
            putString("stockName", stockBody.longName)
            putString("stockSymbol", stockBody.symbol)
            putFloat("stockPriceFloat", stockBody.regularMarketPrice.toFloat())

            // Format price change percentage
            val changeSymbol = if (stockBody.regularMarketChangePercent >= 0) "+" else ""
            putString(
                "stockChange",
                "$changeSymbol${String.format("%.2f%%", stockBody.regularMarketChangePercent)}"
            )

            // Send raw values for ranges
            putFloat("stockDayLow", stockBody.regularMarketDayLow.toFloat())
            putFloat("stockDayHigh", stockBody.regularMarketDayHigh.toFloat())

            // Format market cap
            putString("stockMarketCap", stockBody.marketCap?.let { formatMarketCap(it) } ?: "N/A")

            // Format volume
            putString("stockVolume", formatVolume(stockBody.regularMarketVolume))

            // Format dividend yield
            putString(
                "stockDividendYield",
                stockBody.dividendYield?.let { String.format("%.2f", it) } ?: "N/A")

            putString("fullExchangeName", stockBody.fullExchangeName)

            putString("averageAnalystRating", stockBody.averageAnalystRating)

            // Send raw values for 52 week range
            putFloat("stockLow", stockBody.fiftyTwoWeekLow.toFloat())
            putFloat("stockHigh", stockBody.fiftyTwoWeekHigh.toFloat())

            // Convert dividendDate to readable date and add it to the bundle
            val dividendDate = stockBody.dividendDate
            val dividendDateString = if (dividendDate != null) {
                convertTimestampToDate(dividendDate)
            } else {
                "N/A"
            }
            putString("dividendDate", dividendDateString)
        }
        val detailFragment = DetailFragment()
        detailFragment.arguments = bundle
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    // Function to convert UNIX timestamp to a readable date
    private fun convertTimestampToDate(timestamp: Long): String {
        val date = Date(timestamp * 1000)  // Multiply by 1000 to convert to milliseconds
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return format.format(date)
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

    private fun updateCategoryView(category: String) {
        val categoryText = requireView().findViewById<TextView>(R.id.categoryText)
        val categoryMap = mapOf(
            "Top Gainers" to R.string.top_gainers,
            "Top Losers" to R.string.top_losers,
            "Favorites" to R.string.favorite,
            "All Stocks" to R.string.all_stocks,
            "Saham US" to R.string.us_stock,
            "Saham Indo" to R.string.idn_stock
        )
        categoryText.text = getString(categoryMap[category] ?: R.string.all_stocks)
    }

    private fun displayFilteredStocks(category: String) {
        val filteredStocks = when (category) {
            "Top Gainers" -> filterByTopGainers()
            "Top Losers" -> filterByTopLosers()
            "Favorites" -> filterByFavorites()
            "Saham US" -> filterByMarket("us_market")
            "Saham Indo" -> filterByMarket("id_market")
            else -> viewModel.stocks.value ?: emptyList()
        }
        adapter.updateStocks(filteredStocks)
    }

    private fun filterByTopGainers(): List<Stock> {
        return viewModel.stocks.value.orEmpty().filter {
            it.body.firstOrNull()?.regularMarketChangePercent ?: 0.0 > 0
        }.sortedByDescending {
            it.body.firstOrNull()?.regularMarketChangePercent
        }
    }

    private fun filterByTopLosers(): List<Stock> {
        return viewModel.stocks.value.orEmpty().filter {
            it.body.firstOrNull()?.regularMarketChangePercent ?: 0.0 < 0
        }.sortedBy {
            it.body.firstOrNull()?.regularMarketChangePercent
        }
    }

    private fun filterByFavorites(): List<Stock> {
        return viewModel.stocks.value.orEmpty().filter {
            favoriteSymbols.contains(it.body.firstOrNull()?.symbol)
        }
    }

    private fun filterByMarket(market: String): List<Stock> {
        return viewModel.stocks.value.orEmpty().filter {
            it.body.firstOrNull()?.market == market
        }
    }
}
