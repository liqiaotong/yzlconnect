package com.yunzhiling.yzlconnect.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.yunzhiling.yzlconnect.R
import com.yunzhiling.yzlconnect.adapter.OnWifiListAdapterListener
import com.yunzhiling.yzlconnect.adapter.WifiListAdapter
import com.yunzhiling.yzlconnect.common.Config
import com.yunzhiling.yzlconnect.entity.WifiEntity
import com.yunzhiling.yzlconnect.service.WifiManager
import com.yunzhiling.yzlconnect.service.WifiScanListener
import com.yunzhiling.yzlconnect.service.WifiStatusListener
import com.yunzhiling.yzlconnect.view.AnButton
import kotlinx.android.synthetic.main.layout_confirm_button_yc.*

class WifiListDialog : Dialog {

    private var view: View? = null
    private var wifiAdapter: WifiListAdapter? = null
    private var recyclerview: RecyclerView? = null
    private var refreshLayout: SmartRefreshLayout? = null
    private var wifiProgressBar: ProgressBar? = null
    private var locationManager: LocationManager? = null

    private var listener: OnWifiListDialogListener? = null

    fun setOnWifiListDialogListener(onWifiListDialogListener: OnWifiListDialogListener?) {
        listener = onWifiListDialogListener
    }

    constructor(context: Context) : super(context) {
        initView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view?.let { setContentView(it) }
        initDialog()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        getWifiList()
    }

    private fun initView() {
        view = context?.let { LayoutInflater.from(it).inflate(R.layout.dialog_wifi_list_yc, null) }
        recyclerview = view?.findViewById(R.id.recyclerview)
        refreshLayout = view?.findViewById(R.id.refreshLayout)
        wifiProgressBar = view?.findViewById(R.id.wifi_pb)
        view?.findViewById<AnButton>(R.id.tv_addwifi)?.setOnClickListener {
            listener?.addWifiClick()
        }
        recyclerview?.layoutManager = LinearLayoutManager(context)
        wifiAdapter = WifiListAdapter(context)
        wifiAdapter?.setOnWifiListAdapterListener(object : OnWifiListAdapterListener {
            override fun onItemClick(wifiInfo: WifiEntity?) {
                listener?.wifiSelected(wifiInfo)
            }

            override fun onSearchAgain() {
                wifiAdapter?.cleanData()
                getWifiList()
            }
        })
        recyclerview?.adapter = wifiAdapter
        refreshLayout?.setOnRefreshListener {
            getWifiList()
        }
    }

    private fun initDialog() {
        window?.setGravity(Gravity.CENTER)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.decorView?.setPadding(0, 0, 0, 0)
        val layoutParams = window?.attributes
        layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = layoutParams
    }

    override fun dismiss() {
        wifiAdapter?.cleanData()
        super.dismiss()
    }

    private fun getWifiList() {

        if (wifiAdapter?.getData() == null) {
            recyclerview?.visibility = View.INVISIBLE
            wifiProgressBar?.visibility = View.VISIBLE
        }

        //尝试打开定位
        if (locationManager != null && locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) != true) {
            try {
                Settings.Secure.setLocationProviderEnabled(
                    context.contentResolver,
                    LocationManager.GPS_PROVIDER,
                    false
                )
            } catch (e: Exception) {

            }
        }

        Looper.myLooper()?.let {
            val handler = Handler(it)
            handler?.post {
                WifiManager.openWifi(object : WifiStatusListener {
                    override fun isWifiEnable(isWifiEnable: Boolean) {
                        WifiManager.closeOpenWifi()
                        if (isWifiEnable) {
                            WifiManager.scanWifi(object : WifiScanListener {
                                override fun result(result: List<ScanResult>) {
                                    WifiManager.closeScanWifi()
                                    handler?.postDelayed({
                                        updateList(result)
                                    }, 500)
                                }
                            })
                        } else {
                            handler?.postDelayed({
                                updateList(null)
                            }, 0)
                        }
                    }
                })
            }
        }
    }

    private fun updateList(scanResults: List<ScanResult>?) {
        refreshLayout?.finishRefresh()
        recyclerview?.visibility = View.VISIBLE
        wifiProgressBar?.visibility = View.GONE
        var wifiList: List<WifiEntity>? = scanResults?.filter {
            !TextUtils.isEmpty(it.SSID?.trim()) && !Config.deviceWifis?.any { its -> TextUtils.equals(it.SSID, its.first) }
        }?.sortedBy { it.level.compareTo(it.level) }?.map {
            val wifiLevel = android.net.wifi.WifiManager.calculateSignalLevel(it.level, 100)
            var is2G: Boolean? = false
            var is5G: Boolean? = false
            when (it.frequency) {
                in 2401..2499 -> {
                    is2G = true
                }
                in 4901..5899 -> {
                    is5G = true
                }
            }
            WifiEntity(it.SSID, it.frequency, wifiLevel, is2G, is5G)
        }

        var nWifiList: MutableList<WifiEntity> = ArrayList()
        wifiList?.forEach { iv ->
            val nWifi = nWifiList.firstOrNull { TextUtils.equals(it.ssid, iv.ssid) }
            nWifi?.let {
                if (iv.is2G == true) it.is2G = iv.is2G
                if (iv.is5G == true) it.is5G = iv.is5G
            } ?: run {
                nWifiList.add(iv)
            }
        }

        listener?.scanResult(nWifiList)
        wifiAdapter?.updateData(nWifiList)

    }

}

interface OnWifiListDialogListener {
    fun scanResult(result: MutableList<WifiEntity>?)
    fun wifiSelected(wifiInfo: WifiEntity?)
    fun addWifiClick()
}