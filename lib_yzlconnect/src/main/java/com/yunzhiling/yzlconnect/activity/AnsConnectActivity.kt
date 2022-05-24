package com.yunzhiling.yzlconnect.activity

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.tencent.mmkv.MMKV
import com.yunzhiling.yzlconnect.R
import com.yunzhiling.yzlconnect.adapter.AnsViewPagerAdapter
import com.yunzhiling.yzlconnect.entity.Latlng
import com.yunzhiling.yzlconnect.service.WifiManager
import com.yunzhiling.yzlconnect.view.*
import kotlinx.android.synthetic.main.activity_ans_connect.*

class AnsConnectActivity : AnsCommonActivtiy() {

    private val cursorUnSelect = Color.parseColor("#C2C2C2")
    private val cursorSelected = Color.parseColor("#3789FF")
    private val cursorUnSelectBackground = R.drawable.background_corners_solid_c2c2c2
    private val cursorSelectedBackground = R.drawable.background_corners_solid_3789ff
    private var isAutoConnectMode = false
    private var connectMode:String? = "DefaultMode"
    private var isConnectedSuccess = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ans_connect)
        MMKV.initialize(this)
        WifiManager.init(this)
        initView()
    }

    private fun initView() {
        connectMode = intent.getStringExtra("ConnectMode")
        isAutoConnectMode = TextUtils.equals(connectMode,"AutoConnect")
        back?.setOnClickListener {
            backAction()
        }

        var getLocationView: GetLocationView?
        var getTargetWFView: GetTargetWFView?
        var searchDeviceView: SearchDeviceView? = null
        var sendPWToDeviceView: SendPWToDeviceView? = null
        var connectStatusView: ConnectStatusView? = null

        getLocationView = GetLocationView(this).apply {
            setListener(object : OnGetLocationListener {
                override fun complete(latlng: Latlng?) {
                    sendPWToDeviceView?.setLatlng(latlng)
                    viewPager?.currentItem = 1
                }
            })
        }
        getTargetWFView = GetTargetWFView(this, isAutoConnectMode).apply {
            setListener(object : OnGetTargetWFViewListener {
                override fun complete(
                    connectWifi: Pair<String, String>?,
                    deviceWifi: Pair<String, String>?,
                    isConnectedDevice: Boolean?
                ) {
                    sendPWToDeviceView?.setConnectWifi(connectWifi)
                    searchDeviceView?.setDeviceWifi(deviceWifi)
                    if (isAutoConnectMode) {
                        viewPager?.currentItem = 2
                    } else {
                        if (isConnectedDevice == true) {
                            viewPager?.currentItem = 3
                        } else {
                            viewPager?.currentItem = 2
                        }
                    }
                }
            })
        }
        if (!isAutoConnectMode) {
            searchDeviceView = SearchDeviceView(this).apply {
                setListener(object : OnSearchDeviceListener {
                    override fun complete() {
                        viewPager?.currentItem = 3
                    }
                })
            }
        }
        sendPWToDeviceView = SendPWToDeviceView(this).apply {
            setListener(object : OnSendPWToDeviceListener {
                override fun complete(errorCode: Int) {
                    runOnUiThread {
                        Log.d("yzlconnect","----------->OnConnectFourthListener errorCode $errorCode")
                        when (errorCode) {
                            SendPWToDeviceView.code_success -> {
                                isConnectedSuccess = true
                                connectStatusView?.setStatus(0)
                            }
                            SendPWToDeviceView.code_fail,
                            SendPWToDeviceView.code_connect_error,
                            SendPWToDeviceView.code_search_device_error -> {
                                isConnectedSuccess = false
                                connectStatusView?.setStatus(1)
                            }
                            SendPWToDeviceView.code_timeout -> {
                                isConnectedSuccess = false
                                connectStatusView?.setStatus(2)
                            }
                        }

                        val turnToPage = if (isAutoConnectMode) 3 else 4
                        Log.d("yzlconnect","----------->OnConnectFourthListener turnToPage $turnToPage")
                        viewPager?.setCurrentItem(turnToPage, true)
                    }
                }
            })
        }
        connectStatusView = ConnectStatusView(this).apply {
            setListener(object : OnConnectStatusListener {
                override fun complete(connectAgain: Boolean?) {
                    if (connectAgain == true) {
                        viewPager?.currentItem = 1
                    } else {
                        finish()
                    }
                }
            })
        }

        val views = arrayListOf(
            getLocationView as View?,
            getTargetWFView as View?,
            searchDeviceView as View?,
            sendPWToDeviceView as View?,
            connectStatusView as View?
        )?.filterNotNull()?.toMutableList()

        viewPager?.setCanScroll(false)
        viewPager?.offscreenPageLimit = views.size
        viewPager?.adapter = AnsViewPagerAdapter(views)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {

            }

            override fun onPageSelected(p: Int) {
                cursorProgress(p)
                getLocationView?.viewShow(p == 0)
                getTargetWFView?.viewShow(p == 1)
                if (isAutoConnectMode) {
                    sendPWToDeviceView?.viewShow(p == 2)
                    connectStatusView?.viewShow(p == 3)
                    if (p != 3) {
                        isConnectedSuccess = false
                    }
                } else {
                    searchDeviceView?.viewShow(p == 2)
                    sendPWToDeviceView?.viewShow(p == 3)
                    connectStatusView?.viewShow(p == 4)
                    if (p != 4) {
                        isConnectedSuccess = false
                    }
                }
            }

            override fun onPageScrollStateChanged(p0: Int) {

            }
        })
        cursorProgress(0)

//        sendPWToDeviceView?.setConnectWifi(Pair("ivali.com","ivali1806wf"))
//        viewPager?.currentItem = 2
    }



    private fun cursorProgress(index: Int) {
        if (index in 0..(if (isAutoConnectMode) 3 else 4)) {
            cursor1?.visibility = View.VISIBLE
            cursor2?.visibility = View.VISIBLE
            cursor3?.visibility = View.VISIBLE
            line1?.visibility = View.VISIBLE
            line2?.visibility = View.VISIBLE
            if (isAutoConnectMode) {
                line3?.visibility = View.GONE
                cursor4?.visibility = View.GONE
            } else {
                line3?.visibility = View.VISIBLE
                cursor4?.visibility = View.VISIBLE
            }
            when (index) {
                0 -> {
                    cursor1?.setBackgroundResource(cursorSelectedBackground)
                    cursor2?.setBackgroundResource(cursorUnSelectBackground)
                    cursor3?.setBackgroundResource(cursorUnSelectBackground)
                    cursor4?.setBackgroundResource(cursorUnSelectBackground)
                    line1?.setBackgroundColor(cursorUnSelect)
                    line2?.setBackgroundColor(cursorUnSelect)
                    line3?.setBackgroundColor(cursorUnSelect)
                }
                1 -> {
                    cursor1?.setBackgroundResource(cursorSelectedBackground)
                    cursor2?.setBackgroundResource(cursorSelectedBackground)
                    line1?.setBackgroundColor(cursorSelected)
                    cursor3?.setBackgroundResource(cursorUnSelectBackground)
                    cursor4?.setBackgroundResource(cursorUnSelectBackground)
                    line2?.setBackgroundColor(cursorUnSelect)
                    line3?.setBackgroundColor(cursorUnSelect)
                }
                2 -> {
                    cursor1?.setBackgroundResource(cursorSelectedBackground)
                    cursor2?.setBackgroundResource(cursorSelectedBackground)
                    cursor3?.setBackgroundResource(cursorSelectedBackground)
                    line1?.setBackgroundColor(cursorSelected)
                    line2?.setBackgroundColor(cursorSelected)
                    cursor4?.setBackgroundResource(cursorUnSelectBackground)
                    line3?.setBackgroundColor(cursorUnSelect)
                }
                3 -> {
                    cursor1?.setBackgroundResource(cursorSelectedBackground)
                    cursor2?.setBackgroundResource(cursorSelectedBackground)
                    cursor3?.setBackgroundResource(cursorSelectedBackground)
                    cursor4?.setBackgroundResource(cursorSelectedBackground)
                    line1?.setBackgroundColor(cursorSelected)
                    line2?.setBackgroundColor(cursorSelected)
                    line3?.setBackgroundColor(cursorSelected)
                }
            }
        } else {
            cursor1?.visibility = View.INVISIBLE
            cursor2?.visibility = View.INVISIBLE
            cursor3?.visibility = View.INVISIBLE
            cursor4?.visibility = View.INVISIBLE
            line1?.visibility = View.INVISIBLE
            line2?.visibility = View.INVISIBLE
            line3?.visibility = View.INVISIBLE
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK
            && event.action === KeyEvent.ACTION_DOWN
        ) {
            backAction()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun backAction() {
        if ((viewPager?.currentItem ?: 0) <= 0) {
            finish()
        } else {
            val currentItem = viewPager?.currentItem
            if (isAutoConnectMode) {
                if ((currentItem ?: 0) >= 3) {
                    if (isConnectedSuccess) {
                        finish()
                    } else {
                        viewPager?.currentItem = 1
                    }
                    return
                }
            } else {
                if ((currentItem ?: 0) >= 4) {
                    if (isConnectedSuccess) {
                        finish()
                    } else {
                        viewPager?.currentItem = 1
                    }
                    return
                }
            }
            var newItem = (viewPager?.currentItem ?: 0) - 1
            if (newItem < 0) newItem = 0
            viewPager?.currentItem = newItem
        }
    }

}