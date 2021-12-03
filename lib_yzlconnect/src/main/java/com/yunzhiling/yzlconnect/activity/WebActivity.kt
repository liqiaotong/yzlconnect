package com.yunzhiling.yzlconnect.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.yunzhiling.yzlconnect.R
import kotlinx.android.synthetic.main.activity_web.*

class WebActivity : CommonActivtiy() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        val title = intent.getStringExtra("title")
        val url = intent.getStringExtra("url") ?: ""

        if (!TextUtils.isEmpty(title)) tvTitle.text = title

        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "请求错误", Toast.LENGTH_SHORT).show()
            finish()
        }

        back?.setOnClickListener {
            finish()
        }
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.blockNetworkImage = true
        settings.allowFileAccess = false
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.displayZoomControls = false
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar?.visibility = View.GONE
            }
        }
        webView?.isHorizontalScrollBarEnabled = false
        webView?.setInitialScale(100)
        val chromeClient = WebChromeClient()
        webView?.webChromeClient = chromeClient
        webView?.loadUrl(url)

    }

}