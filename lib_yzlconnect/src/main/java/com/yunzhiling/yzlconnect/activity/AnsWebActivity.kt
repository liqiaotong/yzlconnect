package com.yunzhiling.yzlconnect.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.*
import android.widget.Toast
import com.yunzhiling.yzlconnect.R
import kotlinx.android.synthetic.main.activity_ans_web.*

class AnsWebActivity : AnsCommonActivtiy() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ans_web)

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

        retryTv?.apply {
            setTips("重新加载")
            setOnClickListener {
                setLoading(true)
                webView?.loadUrl(url)
            }
        }

        val settings = webView.settings
        settings?.javaScriptEnabled = true
        settings?.javaScriptCanOpenWindowsAutomatically = true
        settings?.useWideViewPort = true
        settings?.loadWithOverviewMode = true
        settings?.builtInZoomControls = true
        settings?.blockNetworkImage = true
        settings?.allowFileAccess = false
        settings?.domStorageEnabled = true
        settings?.useWideViewPort = true
        settings?.loadWithOverviewMode = true
        settings?.setSupportZoom(true)
        settings?.displayZoomControls = false
        settings?.cacheMode = WebSettings.LOAD_DEFAULT
        settings?.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        webView?.webViewClient = object : WebViewClient() {

            var isLoadError = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isLoadError = false
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                isLoadError = true
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                retryTv?.setLoading(false)
                progressBar?.visibility = View.GONE
                if(isLoadError) {
                    loadError?.visibility = View.VISIBLE
                    webView?.visibility = View.GONE
                }else{
                    loadError?.visibility = View.GONE
                    webView?.visibility = View.VISIBLE
                }
            }
        }
        webView?.isHorizontalScrollBarEnabled = false
        webView?.setInitialScale(100)
        val chromeClient = WebChromeClient()
        webView?.webChromeClient = chromeClient
        webView?.loadUrl(url)

    }

}