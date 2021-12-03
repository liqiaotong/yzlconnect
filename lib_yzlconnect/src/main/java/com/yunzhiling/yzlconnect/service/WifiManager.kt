package com.yunzhiling.yzlconnect.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.core.app.ActivityCompat

class WifiManager {

    companion object {

        const val TYPE_NO_PASSWD = 0x11
        const val TYPE_WEP = 0x12
        const val TYPE_WPA = 0x13

        private var context: Context? = null
        private var wifiManager: WifiManager? = null
        private var wifiReceiver: BroadcastReceiver? = null
        private var connectHandler: Handler? = null
        private var connectRunnable: Runnable? = null
        private var isReceiver = false
        private var isCloseWifiConnect = false

        fun init(context: Context) {
            this.context = context
            if (wifiManager == null) {
                wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            }
        }

        fun closeConnect() {
            isCloseWifiConnect = true
            wifiReceiver?.let {
                try {
                    context?.unregisterReceiver(it)
                } catch (e: Exception) {

                }
            }
            connectRunnable?.let {
                connectHandler?.removeCallbacks(it)
            }
        }

        fun checkWifiConnect(
            ssid: String,
            passwd: String,
            type: Int,
            listener: WifiConnectionListener?
        ) {
            closeConnect()
            isReceiver = false
            isCloseWifiConnect = false
            if (wifiManager?.isWifiEnabled == true) {
                connect(ssid, passwd, type, listener)
            } else {
                openWifi(object : WifiStatusListener {
                    override fun isWifiEnable(isWifiEnable: Boolean) {
                        if (isWifiEnable) {
                            connect(ssid, passwd, type, listener)
                        } else {
                            listener?.connectError()
                        }
                    }
                })
            }
        }

        private var wifiConnectStatusHandler: Handler? = null
        private var wifiConnectStatusRunnable: Runnable? = null

        fun closeCheckWifiConnect() {
            wifiConnectStatusRunnable?.let {
                wifiConnectStatusHandler?.removeCallbacks(it)
            }
        }

        fun checkWifiConnect(
            ssid: String? = "",
            checkTimes: Int? = 30,
            checkInterval: Long? = 1000,
            listener: WifiConnectStatusListener?
        ) {
            var currentTimes = 0
            if (wifiConnectStatusHandler == null) wifiConnectStatusHandler =
                Looper.myLooper()?.let { Handler(it) }
            if (wifiConnectStatusRunnable == null) wifiConnectStatusRunnable = Runnable {
                currentTimes++
                if (currentTimes >= (checkTimes ?: 0)) {
                    wifiConnectStatusRunnable?.let {
                        wifiConnectStatusHandler?.removeCallbacks(it)
                    }
                    listener?.checkFinish()
                } else {
                    //检测当前连接
                    val wifiInfo: WifiInfo? = wifiManager?.connectionInfo
                    wifiInfo?.let {
                        val currentSsid = if (TextUtils.isEmpty(wifiInfo.ssid)) "" else wifiInfo.ssid
                        val targetSsid = "\"$ssid\""
                        val isConnected = TextUtils.equals(currentSsid, targetSsid)
                        listener?.isConnected(isConnected)
                    }
                    wifiConnectStatusRunnable?.let {
                        wifiConnectStatusHandler?.removeCallbacks(it)
                        wifiConnectStatusHandler?.postDelayed(it, checkInterval ?: 1000)
                    }
                }
            }

            wifiConnectStatusRunnable?.let {
                wifiConnectStatusHandler?.removeCallbacks(it)
                wifiConnectStatusHandler?.postDelayed(it, checkInterval ?: 1000)
            }

        }

        fun connect(
            ssid: String,
            password: String,
            type: Int,
            listener: WifiConnectionListener?
        ) {

            if (isCloseWifiConnect) return

            if (TextUtils.isEmpty(ssid)) {
                listener?.connectError()
                return
            }

            if (wifiManager?.isWifiEnabled != true) {
                listener?.connectError()
                return
            }

            //检测当前连接
            val wifiInfo: WifiInfo? = wifiManager?.connectionInfo
            wifiInfo?.let {
                val currentSsid = if (TextUtils.isEmpty(wifiInfo.ssid)) "" else wifiInfo.ssid
                val targetSsid = "\"$ssid\""
                if (TextUtils.equals(currentSsid, targetSsid) && !isCloseWifiConnect) {
                    listener?.isSuccessful(true)
                    return
                }
            }

            //创建一个wifi配置文件
            var wifiConfig = createWifiConfig(ssid, password, type)

            //添加网络
            addNetwork(
                ssid,
                wifiConfig,
                listener
            )

        }

        private fun addNetwork(
            ssid: String,
            wifiConfiguration: WifiConfiguration?,
            listener: WifiConnectionListener?
        ) {
            var connectTimes = 0
            fun connect() {
                if (isCloseWifiConnect) return
                if (connectTimes < 5) {
                    connectTimes++
                    var netId: Int = wifiManager?.addNetwork(wifiConfiguration) ?: -1
                    val hadPermission = context?.let {
                        ActivityCompat.checkSelfPermission(
                            it,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } == PackageManager.PERMISSION_GRANTED
                    if (hadPermission) {
                        if (netId == -1) netId = wifiManager?.configuredNetworks?.let {
                            it.firstOrNull { its -> its.SSID.trim('"') == ssid.trim('"') }?.networkId
                                ?: -1
                        } ?: -1
                    }

                    val isEnableNetwork: Boolean = wifiManager?.enableNetwork(netId, true) ?: false

                    if (!isEnableNetwork) {
                        try {
                            wifiReceiver?.let { context?.unregisterReceiver(it) }
                        } catch (e: Exception) {

                        }
                        if (!isCloseWifiConnect) {
                            listener?.isSuccessful(false)
                        }
                    }
                } else {
                    try {
                        wifiReceiver?.let { context?.unregisterReceiver(it) }
                    } catch (e: Exception) {

                    }
                    if (!isCloseWifiConnect) {
                        listener?.isSuccessful(false)
                    }
                }
            }

            connectRunnable = object : Runnable {
                override fun run() {
                    connectHandler?.removeCallbacks(this)
                    if (!isReceiver) {
                        if (!isCloseWifiConnect) {
                            listener?.isSuccessful(false)
                        }
                        wifiReceiver?.let {
                            try {
                                context?.unregisterReceiver(it)
                            } catch (e: Exception) {

                            }
                        }
                    }
                }
            }

            connectHandler = Looper.myLooper()?.let { Handler(it) }
            connectRunnable?.let {
                connectHandler?.postDelayed(it, 15000)
            }

            wifiReceiver = object : BroadcastReceiver() {
                var broadcastReceiver: BroadcastReceiver = this
                override fun onReceive(context: Context, intent: Intent) {
                    //wifi连接上与否
                    if ((intent.action != null) && (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) && !isCloseWifiConnect) {
                        val info =
                            intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                        if (info != null && info.state == NetworkInfo.State.CONNECTED) {
                            val wifiManager =
                                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                            val wifiInfo = wifiManager.connectionInfo
                            //获取当前wifi名称
                            if (TextUtils.equals("\"${ssid}\"", wifiInfo.ssid)) {
                                isReceiver = true
                                try {
                                    broadcastReceiver?.let {
                                        try {
                                            context?.unregisterReceiver(it)
                                        } catch (e: Exception) {

                                        }
                                    }
                                } catch (e: Exception) {

                                }
                                try {
                                    connectRunnable?.let {
                                        connectHandler?.removeCallbacks(it)
                                    }
                                } catch (e: Exception) {

                                }
                                if (!isCloseWifiConnect) {
                                    listener?.isSuccessful(true)
                                }
                            } else {
                                wifiManager?.disableNetwork(wifiInfo.networkId)
                                if (!isCloseWifiConnect) {
                                    Looper.myLooper()
                                        ?.let { Handler(it).postDelayed({ connect() }, 3500) }
                                }
                            }
                        }
                    }
                }
            }

            context?.registerReceiver(
                wifiReceiver,
                IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            )

            connect()

        }


        private fun createWifiConfig(
            ssid: String,
            password: String,
            type: Int
        ): WifiConfiguration? {
            var config: WifiConfiguration? = null
            val tempConfig: WifiConfiguration? = isWifiConfigExsits(ssid)
            if (tempConfig != null) {
                if (wifiManager?.removeNetwork(tempConfig.networkId) != true) config = tempConfig
            }
            if (null == config) config = WifiConfiguration()
            config.allowedAuthAlgorithms.clear()
            config.allowedGroupCiphers.clear()
            config.allowedKeyManagement.clear()
            config.allowedPairwiseCiphers.clear()
            config.allowedProtocols.clear()
            config.SSID = "\"" + ssid + "\""

            when (type) {
                TYPE_NO_PASSWD -> {
                    //config.wepKeys[0] = ""
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    //config.wepTxKeyIndex = 0
                }
                TYPE_WEP -> {
                    config.hiddenSSID = true
                    config.wepKeys[0] = "\"" + password + "\""
                    config.allowedAuthAlgorithms
                        .set(WifiConfiguration.AuthAlgorithm.SHARED)
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                    config.allowedGroupCiphers
                        .set(WifiConfiguration.GroupCipher.WEP104)
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    config.wepTxKeyIndex = 0
                }
                TYPE_WPA -> {
                    config.preSharedKey = "\"" + password + "\""
                    config.hiddenSSID = true
                    config.allowedAuthAlgorithms
                        .set(WifiConfiguration.AuthAlgorithm.OPEN)
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                    config.allowedPairwiseCiphers
                        .set(WifiConfiguration.PairwiseCipher.TKIP)
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                    config.allowedPairwiseCiphers
                        .set(WifiConfiguration.PairwiseCipher.CCMP)
                    config.status = WifiConfiguration.Status.ENABLED

                    if (getHexKey(password)) {
                        config.wepKeys[0] = password
                    } else {
                        config.wepKeys[0] = "\"" + password + "\""
                    }
                    config.wepTxKeyIndex = 0

                }
            }
            return config
        }

        private fun getHexKey(s: String?): Boolean {
            if (s == null) {
                return false
            }
            val len = s.length
            if (len != 10 && len != 26 && len != 58) {
                return false
            }
            for (i in 0 until len) {
                val c = s[i]
                if (c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F') {
                    continue
                }
                return false
            }
            return true
        }

        private fun isWifiConfigExsits(ssid: String): WifiConfiguration? {
            @SuppressLint("MissingPermission") val existingConfigs: List<WifiConfiguration> =
                wifiManager?.configuredNetworks
                    ?: return null
            for (existingConfig in existingConfigs) {
                if (existingConfig.SSID == "\"" + ssid + "\"") {
                    return existingConfig
                }
            }
            return null
        }

        private var isWifiOpen = false
        private var wifiOpenReceiver: BroadcastReceiver? = null
        private var wifiOpenHandler: Handler? = null
        private var wifiOpenRunnable: Runnable? = null

        fun closeOpenWifi() {
            wifiOpenRunnable?.let {
                wifiOpenHandler?.removeCallbacks(it)
            }
            wifiOpenReceiver?.let {
                try {
                    context?.unregisterReceiver(it)
                } catch (e: Exception) {

                }
            }
        }

        fun openWifi(listener: WifiStatusListener?) {
            closeOpenWifi()
            isWifiOpen = false
            wifiOpenHandler = Looper.myLooper()?.let { Handler(it) }
            wifiOpenRunnable = Runnable {
                if (!isWifiOpen) {
                    wifiReceiver?.let {
                        try {
                            context?.unregisterReceiver(it)
                        } catch (e: Exception) {

                        }
                    }
                    listener?.isWifiEnable(false)
                }
            }

            if (wifiManager?.isWifiEnabled != true) {
                wifiOpenReceiver = object : BroadcastReceiver() {
                    override fun onReceive(contexts: Context?, intent: Intent?) {
                        if (intent?.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                            val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)
                            if (wifiState == WifiManager.WIFI_STATE_ENABLED || wifiState == WifiManager.WIFI_STATE_ENABLED) {
                                isWifiOpen = true
                                wifiOpenRunnable?.let {
                                    wifiOpenHandler?.removeCallbacks(it)
                                }
                                wifiOpenReceiver?.let {
                                    try {
                                        context?.unregisterReceiver(it)
                                    } catch (e: Exception) {

                                    }
                                }
                                listener?.isWifiEnable(true)
                            } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                                wifiOpenRunnable?.let {
                                    wifiOpenHandler?.postDelayed(it, 3500)
                                }
                            }
                        }
                    }
                }
                wifiOpenReceiver?.let {
                    context?.registerReceiver(
                        it,
                        IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
                    )
                }
                wifiManager?.isWifiEnabled = true
            } else {
                listener?.isWifiEnable(true)
            }
        }

        fun closeWifi(onWifiStatusListener: WifiStatusListener) {
            if (wifiManager?.isWifiEnabled == true) {
                var wifiReceiver: BroadcastReceiver = object : BroadcastReceiver() {
                    override fun onReceive(contexts: Context?, intent: Intent?) {
                        if (intent?.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                            val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)
                            if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                                try {
                                    context?.unregisterReceiver(this)
                                } catch (e: Exception) {

                                }
                                onWifiStatusListener.isWifiEnable(false)
                            }
                        }
                    }
                }
                context?.registerReceiver(
                    wifiReceiver,
                    IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
                )
                wifiManager?.isWifiEnabled = false
            } else {
                onWifiStatusListener.isWifiEnable(false)
            }
        }

        private var scanWifiReceiver: BroadcastReceiver? = null

        fun closeScanWifi() {
            scanWifiReceiver?.let {
                try {
                    context?.unregisterReceiver(it)
                } catch (e: Exception) {

                }
            }
        }

        fun scanWifi(listener: WifiScanListener) {
            closeScanWifi()
            scanWifiReceiver = object : BroadcastReceiver() {
                override fun onReceive(contexts: Context?, intent: Intent?) {
                    if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                        listener.result(wifiManager?.scanResults ?: ArrayList())
                        scanWifiReceiver?.let {
                            try {
                                context?.unregisterReceiver(it)
                            } catch (e: Exception) {

                            }
                        }
                    }
                }
            }
            scanWifiReceiver?.let {
                context?.registerReceiver(
                    it,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                )
            }
            wifiManager?.startScan()
        }

        fun getCurrentWifi(context: Context): WifiInfo? {
            val connectManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectManager?.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            if (networkInfo?.isConnected == true) {
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                return wifiManager.connectionInfo
            }
            return null
        }

    }

}

interface WifiConnectionListener {
    fun isSuccessful(isSuccess: Boolean)
    fun connectError()
    fun openWifiError()
}

interface WifiConnectStatusListener {
    fun checkFinish()
    fun isConnected(isConnected: Boolean)
}

interface WifiStatusListener {
    fun isWifiEnable(isWifiEnable: Boolean)
}

interface WifiScanListener {
    fun result(result: List<ScanResult>)
}