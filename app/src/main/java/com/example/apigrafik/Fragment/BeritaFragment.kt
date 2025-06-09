package com.example.apigrafik.Fragment

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.apigrafik.R
import com.example.apigrafik.NewsItem
import com.example.apigrafik.NewsWebViewActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class BeritaFragment : Fragment() {
    private lateinit var newsListLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_berita, container, false)
        newsListLayout = view.findViewById(R.id.newsListLayout)

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
                        displayNews(newsResponse)
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

    private fun displayNews(newsItems: List<NewsItem>) {
        newsListLayout.removeAllViews()

        for (newsItem in newsItems) {
            val newsView = layoutInflater.inflate(R.layout.item_list_berita, newsListLayout, false)

            val imageThumbnail = newsView.findViewById<ImageView>(R.id.imageThumbnail)
            val newsTitle = newsView.findViewById<TextView>(R.id.newsTitle)
            val newsDescription = newsView.findViewById<TextView>(R.id.newsDescription)
            val publishedAt = newsView.findViewById<TextView>(R.id.publishedAt)

            newsTitle.text = newsItem.title
            newsDescription.text = newsItem.description
            publishedAt.text = newsItem.publishedAt.replace("T", " ").replace("Z", "")

            Glide.with(requireContext())
                .load(newsItem.imageUrl?.ifEmpty { null } ?: R.drawable.ic_placeholder)
                .placeholder(R.drawable.ic_placeholder)
                .into(imageThumbnail)

            newsView.setOnClickListener {
                NewsWebViewActivity.start(requireContext(), newsItem.url)
            }

            newsListLayout.addView(newsView)
        }
    }
}
