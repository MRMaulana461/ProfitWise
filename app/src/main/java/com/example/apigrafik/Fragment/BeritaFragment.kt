package com.example.apigrafik.Fragment

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apigrafik.NewsItem
import com.example.apigrafik.NewsWebViewActivity
import com.example.apigrafik.R
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.concurrent.thread

class BeritaFragment : Fragment() {

    private lateinit var newsRecyclerView: RecyclerView
    private lateinit var newsAdapter: NewsAdapter
    private var newsItems = mutableListOf<NewsItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_berita, container, false)

        newsRecyclerView = view.findViewById(R.id.newsRecyclerView)
        newsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        if (isNetworkAvailable()) {
            fetchAndDisplayNewsData()
        } else {
            Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_LONG).show()
        }

        return view
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun fetchAndDisplayNewsData() {
        thread {
            try {
                val apiKey = "f262cdbd84304934b0d8add2dd330c9c"
                val url =
                    "https://newsapi.org/v2/everything?q=stock OR finance OR economy&sortBy=publishedAt&apiKey=$apiKey&language=en"

                val client = OkHttpClient()
                val request = Request.Builder().url(url).get().build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                val newsResponse = if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    parseNewsData(responseBody)
                } else {
                    emptyList<NewsItem>()
                }

                requireActivity().runOnUiThread {
                    if (newsResponse.isEmpty()) {
                        Toast.makeText(requireContext(), "No news available", Toast.LENGTH_LONG).show()
                    } else {
                        updateNewsAdapter(newsResponse)
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch data: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun parseNewsData(responseData: String): List<NewsItem> {
        val newsItems = mutableListOf<NewsItem>()
        try {
            val jsonObject = JSONObject(responseData)
            val articles = jsonObject.getJSONArray("articles")

            for (i in 0 until articles.length()) {
                val article = articles.getJSONObject(i)
                val newsItem = NewsItem(
                    article.optString("title", "No title available"),
                    article.optString("description", "No description available"),
                    article.optString("url", ""),
                    article.optString("publishedAt", "Unknown date"),
                    article.optString("urlToImage", "")
                )
                newsItems.add(newsItem)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newsItems
    }

    private fun updateNewsAdapter(newsList: List<NewsItem>) {
        newsItems.clear()
        newsItems.addAll(newsList)
        newsAdapter = NewsAdapter(requireContext(), newsItems)
        newsRecyclerView.adapter = newsAdapter
    }
}
