package com.example.webviewapp

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.webviewapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // =========================================================================
    // 👉 CHANGE YOUR WEBSITE URL HERE 👈
    // Replace "https://example.com" with your actual website URL.
    // Example: "https://mybusiness.com" or "https://google.com"
    // =========================================================================
    private val targetUrl = "https://harivinder129-art.github.io/AuraCraft/"

    // Launcher for handling HTML5 file uploads (<input type="file">)
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (filePathCallback != null) {
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            filePathCallback?.onReceiveValue(uris)
            filePathCallback = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackNavigation()
        setupWebView()
        setupSwipeToRefresh()
        setupRetryButton()

        // Load your website URL or display the offline error screen
        if (isNetworkAvailable()) {
            binding.webView.loadUrl(targetUrl)
        } else {
            showErrorState(true)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        val settings = webView.settings

        // HTML5 & JavaScript permissions
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        // Cache configuration: fallback to cache when offline
        settings.cacheMode = if (isNetworkAvailable()) {
            WebSettings.LOAD_DEFAULT
        } else {
            WebSettings.LOAD_CACHE_ELSE_NETWORK
        }

        // Enable cookies
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        // WebChromeClient: Handles progress updates and file chooser
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = newProgress
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }

                return try {
                    filePickerLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    this@MainActivity.filePathCallback = null
                    false
                }
            }
        }

        // WebViewClient: Handles navigation, links, and offline error screens
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                showErrorState(false)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Show friendly error view only if the primary page failed to load
                if (request?.isForMainFrame == true) {
                    showErrorState(true)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false

                // Route custom schemes (tel:, mailto:, sms:, WhatsApp, Maps) to native apps
                return if (url.startsWith("http://") || url.startsWith("https://")) {
                    false // Let WebView load web pages normally
                } else {
                    handleExternalScheme(url)
                }
            }
        }

        // Download Listener: Handle file downloads with Android DownloadManager
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
                    addRequestHeader("User-Agent", userAgent)
                    setDescription("Downloading file...")
                    setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimeType)
                    )
                }

                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)
                Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSwipeToRefresh() {
        // Fix: SwipeRefreshLayout wraps a FrameLayout, which doesn't scroll on its own.
        // We explicitly tell SwipeRefreshLayout to check the WebView's scroll position instead,
        // preventing downward scroll gestures from accidentally triggering a page reload.
        binding.swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            binding.webView.canScrollVertically(-1) || binding.webView.scrollY > 0
        }

        // Dynamically enable pull-to-refresh ONLY when at the very top of the page
        binding.webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            binding.swipeRefresh.isEnabled = (scrollY == 0 && !binding.webView.canScrollVertically(-1))
        }

        binding.swipeRefresh.setOnRefreshListener {
            if (isNetworkAvailable()) {
                binding.webView.reload()
            } else {
                binding.swipeRefresh.isRefreshing = false
                showErrorState(true)
            }
        }
    }

    private fun setupRetryButton() {
        binding.btnRetry.setOnClickListener {
            if (isNetworkAvailable()) {
                showErrorState(false)
                binding.webView.reload()
            } else {
                Toast.makeText(this, "Still offline. Please check your internet connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun handleExternalScheme(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(this, "No installed app can handle this action", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showErrorState(showError: Boolean) {
        binding.errorLayout.visibility = if (showError) View.VISIBLE else View.GONE
        binding.webView.visibility = if (showError) View.GONE else View.VISIBLE
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
