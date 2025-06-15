package com.example.apigrafik.Fragment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.apigrafik.NewsItem
import com.example.apigrafik.NewsWebViewActivity
import com.example.apigrafik.R

class NewsAdapter(private val context: Context, private val newsList: List<NewsItem>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_list_berita, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val newsItem = newsList[position]
        holder.newsTitle.text = newsItem.title
        holder.newsDescription.text = newsItem.description
        holder.publishedAt.text = newsItem.publishedAt.replace("T", " ").replace("Z", "")

        Glide.with(context)
            .load(newsItem.imageUrl?.ifEmpty { null } ?: R.drawable.ic_placeholder)
            .placeholder(R.drawable.ic_placeholder)
            .into(holder.imageThumbnail)

        holder.itemView.setOnClickListener {
            NewsWebViewActivity.start(context, newsItem.url)
        }
    }

    override fun getItemCount(): Int = newsList.size

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageThumbnail: ImageView = itemView.findViewById(R.id.imageThumbnail)
        val newsTitle: TextView = itemView.findViewById(R.id.newsTitle)
        val newsDescription: TextView = itemView.findViewById(R.id.newsDescription)
        val publishedAt: TextView = itemView.findViewById(R.id.publishedAt)
    }
}
