package com.yunzhiling.yzlconnect.view

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.net.wifi.ScanResult
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.yunzhiling.yzlconnect.R
import com.yunzhiling.yzlconnect.common.AnsConfig
import com.yunzhiling.yzlconnect.entity.Latlng
import com.yunzhiling.yzlconnect.service.*
import com.yunzhiling.yzlconnect.utils.AnsHandlerHelper
import kotlinx.android.synthetic.main.layout_connect_fourth.view.*

class SendPWToDeviceView : FrameLayout {

    companion object {
        const val code_fail = -1
        const val code_success = 0
        const val code_timeout = 1
        const val code_search_device_error = 2
        const val code_connect_error = 3
    }

    private var isViewShow = false
    private var activity: Activity? = null
    private var listener: OnSendPWToDeviceListener? = null
    private var waveAnimation: ValueAnimator? = null
    private var wifiSsidList: MutableList<String> = ArrayList()
    private var connectWifi: Pair<String, String>? = null
    private var latlng: Latlng? = null

    fun setListener(listener: OnSendPWToDeviceListener?) {
        this.listener = listener
    }

    constructor(activity: Activity) : super(activity) {
        this.activity = activity
        inflate(activity, R.layout.layout_connect_fourth, this)
    }

    fun setLatlng(latlng: Latlng?) {
        this.latlng = latlng
    }

    fun setConnectWifi(connectWifi: Pair<String, String>?) {
        this.connectWifi = connectWifi
    }

    private fun stopWaveAnimation() {
        waveAnimation?.let { it.cancel() }
    }

    private fun startWaveAnimation() {
        if (waveAnimation == null) {
            waveAnimation = ValueAnimator.ofFloat(0f, 0f, 1f, 2f, 3f, 4f, 5f)?.apply {
                duration = 3000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    val progress = it.animatedValue as Float
                    when {
                        progress == 0f -> {
                            wave0?.alpha = 0f
                            wave1?.alpha = 0f
                            wave2?.alpha = 0f
                            wave3?.alpha = 0f
                        }
                        progress <= 1f -> {
                            wave0?.alpha = progress
                        }
                        progress <= 2f -> {
                            wave1?.alpha = progress - 1f
                        }
                        progress <= 3f -> {
                            wave2?.alpha = progress - 2f
                        }
                        progress <= 4f -> {
                            wave3?.alpha = progress - 3f
                        }
                        progress <= 5f -> {
                            val alpha = 1f - (progress - 4f)
                            wave0?.alpha = alpha
                            wave1?.alpha = alpha
                            wave2?.alpha = alpha
                            wave3?.alpha = alpha
                        }
                    }
                }
            }
        }
        if (waveAnimation?.isRunning != true) waveAnimation?.start()
    }

    private fun closeConnect() {
        isCheckDeviceWifi = false
        WifiManager.closeOpenWifi()
        WifiManager.closeScanWifi()
        WifiManager.closeConnect()
        TCPClient.close()
        UDPClient.close()
    }

    private fun connectWifi(delay: Long?) {

        AnsHandlerHelper.loop({
            val nssid = connectWifi?.first ?: ""
            val npassword = connectWifi?.second ?: ""
            sendByUdp(10000,nssid, npassword)
        },5000)


//        fun wifiConnect(deviceWifi: Pair<String, String>?) {
//            Log.d("yzlconnect", "----------->wifi connect")
//            val nssid = connectWifi?.first ?: ""
//            val npassword = connectWifi?.second ?: ""
//            var deviceWifi = deviceWifi
//            if (deviceWifi == null) deviceWifi = AnsConfig.deviceWifis?.firstOrNull()
//            //开始连接wifi
//            WifiManager?.checkWifiConnect(
//                deviceWifi?.first ?: "", deviceWifi?.second ?: "",
//                if (TextUtils.isEmpty(deviceWifi?.second)) {
//                    WifiManager.TYPE_NO_PASSWD
//                } else {
//                    WifiManager.TYPE_WPA
//                }, object : WifiConnectionListener {
//                    override fun isSuccessful(isSuccess: Boolean, ssid: String?, password: String?) {
//                        if (isSuccess) {
//                            connectDeviceSuccess()
//                            var port = if (!wifiSsidList.contains(ssid)) 10000 else 9999
//                            //设备通信
//                            sendByUdp(port, nssid, npassword)
//                            //sendByTcp(port, ssid, password)
//                        } else {
//                            searchDeviceError()
//                        }
//                    }
//
//                    override fun connectError() {
//                        searchDeviceError()
//                    }
//
//                    override fun openWifiError() {
//                        connectDeviceError()
//                    }
//                }
//            )
//        }
//
//        Log.d("yzlconnect", "----------->connectWifi delay:$delay")
//
//        AnsHandlerHelper.loop({
//            fun connect(deviceWifi: Pair<String, String>?) {
//                Log.d("yzlconnect", "----------->check device wifi finish")
//                wifiConnect(deviceWifi)
//            }
//            checkDeviceWifi({
//                searchDeviceError()
//            }, { its ->
//                connect(its)
//            })
//        }, delay ?: 0)

    }

    private fun connectDeviceError() {
        activity?.runOnUiThread {
            listener?.complete(code_connect_error)
        }
    }

    private fun searchDeviceError() {
        activity?.runOnUiThread {
            listener?.complete(code_search_device_error)
        }
    }

    private fun sendByTcp(port: Int, ssid: String, password: String) {
        try {
            TCPClient.tcpConnect(
                port,
                ssid,
                password,
                latlng?.lng ?: 0.0,
                latlng?.lat ?: 0.0,
                object : ConnectCallback {
                    override fun connected() {
                        connectSuccess()
                    }

                    override fun sended() {
                        sendToDeviceSuccess()
                    }

                    override fun fail() {
                        TCPClient.close()
                        connectFail()
                    }

                    override fun timeout() {
                        connectTimeout()
                    }
                })
        } catch (e: Exception) {
            connectFail()
        }

    }

    fun sendByUdp(port: Int, ssid: String, password: String) {
        try {
            UDPClient.udpConnect(port, ssid, password, latlng?.lng ?: 0.0,
                latlng?.lat ?: 0.0, object : ConnectCallback {
                    override fun connected() {
                        connectSuccess()
                    }

                    override fun sended() {
                        sendToDeviceSuccess()
                    }

                    override fun fail() {
                        UDPClient.close()
                        //切换Tcp请求
                        sendByTcp(port, ssid, password)
                    }

                    override fun timeout() {
                        Log.d("yzlconnect", "----------->ui timeout")
                        connectTimeout()
                    }
                })
        } catch (e: Exception) {
            connectFail()
        }
    }

    private fun restoreStyle() {
        loading1?.visibility = View.GONE
        loading1?.visibility = View.VISIBLE
        image1?.visibility = View.GONE
        loading2?.visibility = View.GONE
        loading2?.visibility = View.VISIBLE
        image2?.visibility = View.GONE
        loading3?.visibility = View.GONE
        loading3?.visibility = View.VISIBLE
        image3?.visibility = View.GONE
    }

    private fun connectTimeout() {
        activity?.runOnUiThread {
            listener?.complete(code_timeout)
        }
    }

    private fun connectDeviceSuccess() {
        activity?.runOnUiThread {
            loading1?.visibility = View.GONE
            image1?.visibility = View.VISIBLE
        }
    }

    private fun sendToDeviceSuccess() {
        activity?.runOnUiThread {
            loading2?.visibility = View.GONE
            image2?.visibility = View.VISIBLE
        }
    }

    private fun connectSuccess() {
        activity?.runOnUiThread {
            loading3?.visibility = View.GONE
            image3?.visibility = View.VISIBLE
            AnsHandlerHelper.loop({ listener?.complete(code_success) }, 800)
        }
    }

    private fun connectFail() {
        activity?.runOnUiThread {
            listener?.complete(code_fail)
        }
    }

    private var isCheckDeviceWifi = false

    private fun checkDeviceWifi(checkDeviceWifiNullAction: (() -> Unit)?, nextAction: ((deviceWifi: Pair<String, String>?) -> Unit)?) {

        isCheckDeviceWifi = true

        Log.d("message", "check devices wifi")
        var currentCheckTimes = 0
        fun check(delay: Long?) {
            if (!isCheckDeviceWifi) return
            Looper.myLooper()?.let { its ->
                Handler(its).postDelayed({
                    WifiManager.openWifi(object : WifiStatusListener {
                        override fun isWifiEnable(isWifiEnable: Boolean) {
                            WifiManager.closeOpenWifi()
                            Log.d("yzlconnect", "check devices wifi isWifiEnable:$isWifiEnable")
                            if (!isCheckDeviceWifi) return
                            if (isWifiEnable) {
                                //wifi打开后获取列表
                                WifiManager.scanWifi(object : WifiScanListener {
                                    override fun result(result: List<ScanResult>) {
                                        WifiManager.closeScanWifi()
                                        if (!isCheckDeviceWifi) return
                                        wifiSsidList?.clear()
                                        wifiSsidList?.addAll(result?.map { it.SSID })
                                        //当前包含设备wifi
                                        var deviceWifi: Pair<String, String>? = null
                                        run block@{
                                            AnsConfig.deviceWifis?.forEach {
                                                val configDeviceWifiSSID = it.first
                                                val configDeviceWifiPWD = it.second
                                                result?.forEach { its ->
                                                    val searchWifiSSID = its.SSID
                                                    if (searchWifiSSID?.contains(
                                                            configDeviceWifiSSID,
                                                            false
                                                        ) == true
                                                    ) {
                                                        deviceWifi =
                                                            Pair(searchWifiSSID, configDeviceWifiPWD)
                                                        return@block
                                                    }
                                                }
                                            }
                                        }
                                        Log.d("yzlconnect", "check devices wifi device:${deviceWifi?.first ?: "null"}")
                                        deviceWifi?.let {
                                            nextAction?.let {
                                                it(deviceWifi)
                                            }
                                        } ?: run {
                                            if (currentCheckTimes < 10) {
                                                currentCheckTimes++
                                                check(3000)
                                            } else {
                                                checkDeviceWifiNullAction?.let {
                                                    it()
                                                }
                                            }
                                        }
                                    }
                                })
                            } else {
                                nextAction?.let {
                                    it(null)
                                }
                            }
                        }
                    })
                }, delay ?: 0)
            } ?: run {
                checkDeviceWifiNullAction?.let {
                    it()
                }
            }
        }

        check(0)
    }

    fun viewShow(isShow: Boolean) {

        closeConnect()

        if (isShow) {
            isViewShow = true
            restoreStyle()
            startWaveAnimation()
            connectWifi(300)
        } else {
            if (isViewShow) {
                isViewShow = false
                stopWaveAnimation()
            }
        }

    }

}

interface OnSendPWToDeviceListener {
    fun complete(errorCode: Int)
}