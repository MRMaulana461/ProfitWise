package com.example.apigrafik

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class NewsWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    companion object {
        fun start(context: Context, newsUrl: String) {
            val intent = Intent(context, NewsWebViewActivity::class.java)
            intent.putExtra("NEWS_URL", newsUrl)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_webview)

        webView = findViewById(R.id.webView)
        val backIcon: ImageView = findViewById(R.id.backIcon2)

        setupWebView()

        val newsUrl = intent.getStringExtra("NEWS_URL") ?: ""
        if (newsUrl.isNotEmpty()) {
            loadUrl(newsUrl)
        } else {
            Log.e("NewsWebViewActivity", "Invalid URL: $newsUrl")
            showError("Invalid URL")
        }

        backIcon.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("WebView", "Loading URL: $url")
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d("WebView", "Finished loading URL: $url")
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                Log.e("WebView", "Error loading page: ${error.description}")

                // Only handle main frame errors
                if (request.isForMainFrame) {
                    runOnUiThread {
                        showError("Failed to load page: ${error.description}")
                    }
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                return if (url.startsWith("tel:") || url.startsWith("mailto:")) {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true
                } else {
                    false
                }
            }
        }

        webView.setOnLongClickListener { true }  // Disable long press
    }

    private fun loadUrl(url: String) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                webView.loadUrl("https://$url")
            } else {
                webView.loadUrl(url)
            }
        } catch (e: Exception) {
            Log.e("WebView", "Error loading URL: ${e.message}")
            showError("Error loading URL: ${e.message}")
        }
    }

    private fun showError(message: String) {
        // Load error page in WebView
        val errorHtml = """
            <html>
                <body style="display:flex;justify-content:center;align-items:center;height:100vh;margin:0;background-color:#f5f5f5;">
                    <div style="text-align:center;padding:20px;">
                        <h2 style="color:#666;">Error</h2>
                        <p style="color:#999;">$message</p>
                        <button onclick="window.location.reload()" style="padding:10px 20px;background-color:#007AFF;color:white;border:none;border-radius:5px;margin-top:10px;">
                            Retry
                        </button>
                    </div>
                </body>
            </html>
        """.trimIndent()
        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}