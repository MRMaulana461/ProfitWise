package com.example.apigrafik

data class NewsItem (
    val title: String,
    val description: String,
    val url: String,
    val publishedAt: String,
    val imageUrl: String? = null
)
