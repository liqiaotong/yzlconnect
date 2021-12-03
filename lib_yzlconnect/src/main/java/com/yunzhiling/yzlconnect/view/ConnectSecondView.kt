package com.yunzhiling.yzlconnect.view

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.wifi.ScanResult
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import com.yunzhiling.yzlconnect.R
import com.yunzhiling.yzlconnect.common.Config
import com.yunzhiling.yzlconnect.dialog.OnWifiListDialogListener
import com.yunzhiling.yzlconnect.dialog.WifiListDialog
import com.yunzhiling.yzlconnect.entity.WifiEntity
import com.yunzhiling.yzlconnect.service.WifiManager
import com.yunzhiling.yzlconnect.service.WifiScanListener
import com.yunzhiling.yzlconnect.service.WifiStatusListener
import kotlinx.android.synthetic.main.layout_connect_second.view.*
import kotlinx.android.synthetic.main.layout_connect_second.view.layout
import kotlinx.android.synthetic.main.layout_connect_second.view.next
import kotlinx.android.synthetic.main.layout_connect_second.view.title
import kotlinx.android.synthetic.main.layout_connect_thrid.view.*

class ConnectSecondView : FrameLayout {

    private var isViewShow = false
    private var activity: Activity? = null
    private var isAddWifi = false
    private var selectWifiInfo: WifiEntity? = null
    private var mWifiListDialog: WifiListDialog? = null
    private var wifiInfoList: MutableList<WifiEntity> = ArrayList()
    private var listener: OnConnectSecondListener? = null
    private var isAutoConnectMode: Boolean? = false

    fun setListener(listener: OnConnectSecondListener?) {
        this.listener = listener
    }

    constructor(activity: Activity, isAutoConnectMode: Boolean? = false) : super(activity) {
        this.activity = activity
        this.isAutoConnectMode = isAutoConnectMode
        inflate(activity, R.layout.layout_connect_second, this)
        initView()
    }

    private fun initView() {
        title?.text = if (isAutoConnectMode == true) "设备目标WiFi" else "设备网络配置"
        next?.apply {
            setTips(if (isAutoConnectMode == true) "立即配置" else "下一步")
            setOnClickListener {
                next()
            }
        }
        openWifiList?.setOnClickListener {
            showWifiList()
        }
    }

    private fun showWifiList() {
        layout?.visibility = View.GONE
        if (mWifiListDialog == null) {
            activity?.let {
                mWifiListDialog = WifiListDialog(it)
            }
        }
        mWifiListDialog?.setOnDismissListener {
            if (isAddWifi) {
                //手动输入模式
                updateSelectWifiStyle("")
                setSsidEditTextInputMode(true)
                //调起输入法
                Looper.myLooper()?.let {
                    Handler(it).postDelayed({ showSoftKeyboard(this) }, 500)
                } ?: run {
                    showSoftKeyboard(this)
                }
            } else {
                //自动填充模式
                updateSelectWifiStyle(selectWifiInfo?.ssid ?: "", selectWifiInfo?.frequency ?: 0, selectWifiInfo?.isMix)
                setSsidEditTextInputMode(false)
                passwordEt?.apply {
                    if (!TextUtils.isEmpty(ssidEt?.text ?: "")) {
                        requestFocus()
                        //调起输入法
                        Looper.myLooper()?.let {
                            Handler(it).postDelayed({ showSoftKeyboard(this) }, 500)
                        } ?: run {
                            showSoftKeyboard(this)
                        }
                    }
                }
            }
            layout?.visibility = View.VISIBLE
        }
        mWifiListDialog?.setOnWifiListDialogListener(object : OnWifiListDialogListener {
            override fun scanResult(result: MutableList<WifiEntity>?) {
                result?.let {
                    wifiInfoList.clear()
                    wifiInfoList.addAll(result)
                }
            }

            override fun wifiSelected(wifiInfo: WifiEntity?) {
                isAddWifi = false
                selectWifiInfo = wifiInfo
                mWifiListDialog?.dismiss()
            }

            override fun addWifiClick() {
                isAddWifi = true
                mWifiListDialog?.dismiss()
            }

        })
        if (mWifiListDialog?.isShowing != true) {
            mWifiListDialog?.show()
        }
    }

    private fun setSsidEditTextInputMode(isInput: Boolean? = true) {

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (selectWifiInfo == null) selectWifiInfo = WifiEntity()
                selectWifiInfo?.apply {
                    ssid = s?.toString() ?: ""
                    is2G = false
                    is5G = false
                    frequency = 0
                    level = 0
                }
            }
        }

        ssidEt?.apply {
            if (isInput == true) {
                isFocusable = true
                isFocusableInTouchMode = true
                hint = "请输入网络名称"
                setOnClickListener(null)
                requestFocus()
                addTextChangedListener(textWatcher)
            } else {
                isFocusable = false
                isFocusableInTouchMode = false
                hint = "请选择WiFi网络"
                setOnClickListener {
                    showWifiList()
                }
                removeTextChangedListener(textWatcher)
            }
        }
    }

    private fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun closeSoftKeyboard() {
        var imm: InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive && activity?.currentFocus != null) {
            activity?.currentFocus?.windowToken?.let {
                imm.hideSoftInputFromWindow(it, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }
    }

    private fun initWifiInfo() {

        next?.setLoading(false)
        fun showWifiListAction() {
            Looper.myLooper()?.let {
                Handler(it).postDelayed({ showWifiList() }, 500)
            } ?: run {
                showWifiList()
            }
        }

        setSsidEditTextInputMode(false)

        if (selectWifiInfo == null) {

            activity?.let {
                WifiManager.getCurrentWifi(it)?.let { its ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        var ssid = its.ssid
                        if (ssid.length > 1) ssid = ssid.replaceRange(0, 1, "")
                        if (ssid.length > 2) ssid = ssid.replaceRange(ssid.length - 1, ssid.length, "")
                        if (Config.deviceWifis?.any { ita -> TextUtils.equals(ita.first, ssid) }) {
                            showWifiListAction()
                        } else {
                            var is2G: Boolean? = false
                            var is5G: Boolean? = false
                            when (its.frequency) {
                                in 2401..2499 -> {
                                    is2G = true
                                }
                                in 4901..5899 -> {
                                    is5G = true
                                }
                            }
                            if (is2G == true) {
                                selectWifiInfo = WifiEntity(ssid, its.frequency, 0, is2G, is5G)
                                updateSelectWifiStyle(
                                    selectWifiInfo?.ssid ?: "",
                                    selectWifiInfo?.frequency,
                                    selectWifiInfo?.isMix
                                )
                            } else {
                                updateSelectWifiStyle("")
                                showWifiListAction()
                            }
                        }
                    } else {
                        showWifiListAction()
                    }
                } ?: run {
                    showWifiListAction()
                }
            } ?: run {
                showWifiListAction()
            }
        }
    }

    private var isCurrentSelected5GWifi = false

    private fun updateSelectWifiStyle(ssid: String, frequency: Int? = 0, isMix: Boolean? = null) {

        val tips2G = "请确保门店WiFi为2.4G频段或混频"
        val tips5G = "当前WiFi频段为5G，不能配置设备， 请选择其他2.4G频段WiFi"

        ssidEt?.setText(ssid)

        when (frequency) {
            in 2401..2499 -> {
                tips?.text = tips2G
                tips?.setTextColor(Color.parseColor("#3789FF"))
                isCurrentSelected5GWifi = false
            }
            in 4901..5899 -> {
                if (isMix == true) {
                    tips?.text = tips2G
                    tips?.setTextColor(Color.parseColor("#3789FF"))
                } else {
                    tips?.text = tips5G
                    tips?.setTextColor(Color.parseColor("#FF6400"))
                }
                isCurrentSelected5GWifi = true
            }
            else -> {
                tips?.text = tips2G
                tips?.setTextColor(Color.parseColor("#3789FF"))
                isCurrentSelected5GWifi = false
            }
        }

        if(isMix==true) isCurrentSelected5GWifi = false

        if(isCurrentSelected5GWifi){
            next?.alpha = 0.2f
        }else{
            next?.alpha = 1f
        }

    }

    private fun next() {
        val ssid = ssidEt?.text?.toString() ?: ""
        val password = passwordEt?.text?.toString() ?: ""
        if (TextUtils.isEmpty(ssid)) {
            Toast.makeText(context, "请设置Wifi名称", Toast.LENGTH_SHORT).show()
            return
        }
        val regex = Regex("[{}\":,]")
        if(regex.containsMatchIn(input = ssid) ){
            Toast.makeText(context, "网络名称不能存在以下字符：{}\":,", Toast.LENGTH_SHORT).show()
            return
        }
        if(regex.containsMatchIn(password)){
            Toast.makeText(context, "密码不能存在以下字符：{}\":,", Toast.LENGTH_SHORT).show()
            return
        }

        if(next?.alpha?:0f < 1f){
            return
        }

        next?.setLoading(true)
        checkDeviceWifi {
            next?.setLoading(false)
            var currentWifiSSID: String? = null
            WifiManager.getCurrentWifi(context)?.let { its ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    var ssid = its.ssid
                    if (ssid.length > 1) ssid = ssid.replaceRange(0, 1, "")
                    if (ssid.length > 2) ssid = ssid.replaceRange(ssid.length - 1, ssid.length, "")
                    currentWifiSSID = ssid
                }
            }
            listener?.complete(Pair(ssid, password), it, TextUtils.equals(currentWifiSSID, it?.first))
        }
    }

    fun viewShow(isShow: Boolean) {
        if (isShow) {
            isViewShow = true
            initWifiInfo()
        } else {
            if (isViewShow) {
                isViewShow = false
                WifiManager.closeOpenWifi()
                WifiManager.closeScanWifi()
            }
        }
    }

    private fun checkDeviceWifi(nextAction: ((deviceWifi: Pair<String, String>?) -> Unit)?) {
        Looper.myLooper()?.let { its ->
            Handler(its).post {
                WifiManager.openWifi(object : WifiStatusListener {
                    override fun isWifiEnable(isWifiEnable: Boolean) {
                        if (isWifiEnable) {
                            //wifi打开后获取列表
                            WifiManager.scanWifi(object : WifiScanListener {
                                override fun result(result: List<ScanResult>) {
                                    //当前包含设备wifi
                                    val deviceWifi = Config.deviceWifis?.firstOrNull {
                                        result?.any { its ->
                                            its.SSID?.contains(it.first, false) ?: false
                                        }
                                    }
                                    nextAction?.let {
                                        it(deviceWifi)
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
            }
        } ?: run {
            nextAction?.let {
                it(null)
            }
        }
    }

}

interface OnConnectSecondListener {
    fun complete(connectWifi: Pair<String, String>?, deviceWifi: Pair<String, String>?, isConnectedDevice: Boolean? = false)
}