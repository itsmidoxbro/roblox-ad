package xyz.justsal.blox

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(17, 17, 17)
        window.navigationBarColor = android.graphics.Color.rgb(17, 17, 17)

        val webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(17, 17, 17))

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false

            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    url: String
                ): Boolean {
                    return !url.startsWith("file:///android_asset/") &&
                           !url.startsWith("https://")
                }
            }

            addJavascriptInterface(RobloxBridge(), "SalBloxNative")
        }

        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    private inner class RobloxBridge {

        @JavascriptInterface
        fun get(rawUrl: String): String? {
            return try {
                val url = URL(rawUrl)
                val host = url.host.lowercase()

                val allowed =
                    host == "roblox.com" ||
                    host.endsWith(".roblox.com")

                if (url.protocol != "https" || !allowed) {
                    return null
                }

                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "SalBlox-Android")
                    instanceFollowRedirects = true
                }

                try {
                    val code = conn.responseCode
                    val stream =
                        if (code in 200..299) conn.inputStream
                        else conn.errorStream

                    val body = stream?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() } ?: return null

                    if (code !in 200..299) null else body
                } finally {
                    conn.disconnect()
                }

            } catch (_: Exception) {
                null
            }
        }
    }
}
