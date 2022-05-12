package com.yunzhiling.yzlconnect.view

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.yunzhiling.yzlconnect.R
import com.yunzhiling.yzlconnect.common.AnsConfig
import com.yunzhiling.yzlconnect.service.WifiManager
import kotlinx.android.synthetic.main.layout_connect_thrid.view.*

class SearchDeviceView : FrameLayout {

    private var isViewShow = false
    private val tips0 = "点击右侧链接按钮，前往WiFi设置界面，连接yzl_smart_bell或A01_YZL热点，返回程序"
    private val tips1 = "点击右侧链接按钮，前往WiFi设置界面，连接yzl_smart_bell热点，返回程序"
    private val tips2 = "点击右侧链接按钮，前往WiFi设置界面，连接A01_YZL热点，返回程序"
    private val tips3 = "yzl_smart_bell或A01_YZL"
    private val tips4 = "yzl_smart_bell"
    private val tips5 = "A01_YZL"
    private val tips6 = "点击立即配置，开始配网"
    private val devicePassword = "ivali_yzl_2020"
    private var activity: Activity? = null
    private var deviceWifi: Pair<String, String>? = null
    private var listener: OnSearchDeviceListener? = null
    private var ccHandler: Handler? = null
    private var ccRunnable: Runnable? = null

    fun setListener(listener: OnSearchDeviceListener?) {
        this.listener = listener
    }

    constructor(activity: Activity) : super(activity) {
        this.activity = activity
        inflate(activity, R.layout.layout_connect_thrid, this)
        initView()
    }

    private fun initView() {
        next?.apply {
            setTips("立即配置")
            setOnClickListener {
                if (next?.alpha ?: 0f >= 1f) {
                    next()
                }
            }
        }
        password?.text = devicePassword
        passwordCopy?.setOnClickListener {
            copyToClipboard(devicePassword)
        }
        connect?.setOnClickListener {
            turnWifiSetting()
        }
    }

    private fun copyToClipboard(content: String) {
        try {
            val clipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("yzl copy", content)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(context, "已复制到粘贴板", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "复制失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun turnWifiSetting() {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    fun setDeviceWifi(deviceWifi: Pair<String, String>?) {
        this.deviceWifi = deviceWifi
        updateStyle()
    }

    private fun startCheckWifiConnect() {

        fun isWifiConnect() {
            activity?.let {
                WifiManager.getCurrentWifi(it)?.let { its ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        var ssid = its.ssid
                        if (ssid.length > 1) ssid = ssid.replaceRange(0, 1, "")
                        if (ssid.length > 2) ssid =
                            ssid.replaceRange(ssid.length - 1, ssid.length, "")
                        if (deviceWifi == null) {
                            let let@{
                                AnsConfig.deviceWifis?.forEach { iit ->
                                    if (ssid.contains(iit.first)) {
                                        deviceWifi = iit
                                        return@let
                                    }
                                }
                            }
                        }
                        val isSameWifi = deviceWifi?.first?.let { ivs -> ssid?.contains(ivs) }
                        setNextButtonStyle(isSameWifi == true)
                    } else {
                        setNextButtonStyle(true)
                    }
                }
                updateStyle()
            }
        }

        fun startCheck(delay: Long? = null) {
            ccRunnable?.let {
                ccHandler?.removeCallbacks(it)
                ccHandler?.postDelayed(it, delay ?: 0)
            }
        }

        if (ccHandler == null) ccHandler = Looper.myLooper()?.let { Handler(it) }
        if (ccRunnable == null) ccRunnable = Runnable {
            isWifiConnect()
            startCheck(2000)
        }
        startCheck()
    }

    private fun stopCheckWifiConnect() {
        ccRunnable?.let {
            ccHandler?.removeCallbacks(it)
        }
    }

    private fun setNextButtonStyle(isAllowToNext: Boolean) {
        if (isAllowToNext) {
            next?.alpha = 1f
        } else {
            next?.alpha = 0.2f
        }
    }

    private fun updateStyle() {

        fun defaultStyle() {
            passwordLayout?.visibility = View.VISIBLE
            centerTips?.text = "2、$tips0"
            centerTipsmv?.text = tips3
            lastTips?.text = "3、$tips6"
        }

        fun smart() {
            passwordLayout?.visibility = View.VISIBLE
            centerTips?.text = "2、$tips1"
            centerTipsmv?.text = tips4
            lastTips?.text = "3、$tips6"
        }

        fun a01() {
            passwordLayout?.visibility = View.GONE
            centerTips?.text = "1、$tips2"
            centerTipsmv?.text = tips5
            lastTips?.text = "2、$tips6"
        }

        deviceWifi?.let {
            when {
                TextUtils.equals(it.first, "yzl_smart_bell") -> {
                    smart()
                }
                TextUtils.equals(it.first, "A01_YZL") -> {
                    a01()
                }
                it.first.contains("A01_YZL") -> {
                    a01()
                }
                else -> {
                    defaultStyle()
                }
            }
        } ?: run {
            defaultStyle()
        }
    }

    fun next() {
        listener?.complete()
    }

    fun viewShow(isShow: Boolean) {
        if (isShow) {
            isViewShow = true
            updateStyle()
            startCheckWifiConnect()
        } else {
            if (isViewShow) {
                isViewShow = false
                stopCheckWifiConnect()
            }
        }
    }
}

interface OnSearchDeviceListener {
    fun complete()
}