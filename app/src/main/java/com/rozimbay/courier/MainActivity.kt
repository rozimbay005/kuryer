package com.rozimbay.courier

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    companion object {
        const val BASE_URL = "https://rozimbay.uz"
        const val LOGIN_URL = "$BASE_URL/courier/login.php"
        const val PREFS = "courier_prefs"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val uri = Uri.parse(url)
                return if (uri.host != null && uri.host == Uri.parse(BASE_URL).host) {
                    false
                } else {
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (url.contains("/courier/") && !url.contains("login.php")) {
                    onLoggedIn()
                }
                if (url.contains("login.php")) {
                    stopService(Intent(this@MainActivity, OrderPollingService::class.java))
                }
            }
        }

        requestNotificationPermissionIfNeeded()
        webView.loadUrl(LOGIN_URL)
    }

    private fun onLoggedIn() {
        val cookie = CookieManager.getInstance().getCookie(BASE_URL)
        if (!cookie.isNullOrEmpty()) {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString("session_cookie", cookie)
                .apply()

            val serviceIntent = Intent(this, OrderPollingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200
                )
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
