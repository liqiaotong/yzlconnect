package com.yunzhiling.yzlconnect.view

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.permissionx.guolindev.PermissionX
import com.tencent.map.geolocation.TencentLocation
import com.tencent.map.geolocation.TencentLocationListener
import com.tencent.map.geolocation.TencentLocationManager
import com.tencent.map.geolocation.TencentLocationRequest
import com.yunzhiling.yzlconnect.R
import com.yunzhiling.yzlconnect.entity.Latlng
import com.yunzhiling.yzlconnect.utils.SharedPreferenceUtils
import kotlinx.android.synthetic.main.layout_connect_first.view.*

class ConnectFirstView : FrameLayout {

    private var isViewShow = false
    private var activity: AppCompatActivity? = null
    private var alphaAnimation: ObjectAnimator? = null
    private var mLocationManager: TencentLocationManager? = null
    private var locationListener: TencentLocationListener? = null
    private var noPermissionDialog: ActionConfirmDialog? = null
    private var listener: OnConnectFirstListener? = null
    private var latlng: Latlng? = null
    private var isTurn: Boolean = false

    fun setListener(listener: OnConnectFirstListener?) {
        this.listener = listener
    }

    constructor(activity: AppCompatActivity) : super(activity) {
        this.activity = activity
        inflate(activity, R.layout.layout_connect_first, this)
        initView()
    }

    private fun initView() {
        next?.apply {
            setTips("下一步")
            setOnClickListener {
                next()
            }
        }
        noPermissionDialog = ActionConfirmDialog(context)?.apply {
            setListener(object : OnActionConfirmDialogListener {
                override fun confirm(dialog: ActionConfirmDialog) {
                    dialog.dismiss()
                }
            })
        }
        startLightAnimation()
        Looper.myLooper()?.let {
            Handler(it).postDelayed({ getPermission() }, 300)
        } ?: run {
            getPermission()
        }
        getLocation()
    }

    @SuppressLint("WrongConstant")
    private fun startLightAnimation() {
        light?.let {
            if (alphaAnimation == null) {
                alphaAnimation = ObjectAnimator.ofFloat(it, "alpha", 0f, 1f, 0f)?.apply {
                    duration = 1500
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                }
            }
            if (alphaAnimation?.isRunning != true) alphaAnimation?.start()
        }
    }

    private fun getLocation() {

        val locationTimestamp = SharedPreferenceUtils.getLong("location_timestamp")
        if (locationTimestamp != null && locationTimestamp > 0 && (System.currentTimeMillis() - locationTimestamp) <= (1000*60*15/*15分钟*/)) {
            val locationLat = SharedPreferenceUtils.getFloat("location_lat")
            val locationLng = SharedPreferenceUtils.getFloat("location_lng")
            if (locationLat > 0 && locationLng > 0) {
                latlng = Latlng(locationLat.toDouble(), locationLng.toDouble())
            }else{
                latlng = null
            }
        }else{
                latlng = null
        }

        if (latlng == null) {
            mLocationManager?.removeUpdates(locationListener)
            mLocationManager = TencentLocationManager.getInstance(context)
            val request = TencentLocationRequest.create()
            request.interval = 2000
            request.requestLevel = TencentLocationRequest.REQUEST_LEVEL_ADMIN_AREA
            request.isAllowGPS = true
            locationListener = object : TencentLocationListener {
                override fun onLocationChanged(p0: TencentLocation?, p1: Int, p2: String?) {
                    SharedPreferenceUtils.setLong("location_timestamp", System.currentTimeMillis())
                    SharedPreferenceUtils.setFloat("location_lat", p0?.latitude?.toFloat() ?: 0.0f)
                    SharedPreferenceUtils.setFloat("location_lng", p0?.longitude?.toFloat() ?: 0.0f)
                    latlng = Latlng(p0?.latitude ?: 0.0, p0?.longitude ?: 0.0)
                    locationListener?.let {
                        mLocationManager?.removeUpdates(it)
                    }
                    if (next?.isLoading() == true && !isTurn) {
                        complete()
                    }
                }

                override fun onStatusUpdate(p0: String?, p1: Int, p2: String?) {

                }
            }
            locationListener?.let {
                mLocationManager?.requestLocationUpdates(request, it)
            }
        }
    }

    private fun getPermission(allGrantedFunction: ((Boolean) -> Unit)? = null) {
        //获取定位权限,获取WIFI权限
        PermissionX.init(activity).permissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_SECURE_SETTINGS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ).request { allGranted: Boolean, _: List<String?>?, _: List<String?>? ->
            allGrantedFunction?.let {
                it(allGranted)
            }
        }
    }

    private fun checkPermission(function: (() -> Unit)? = null) {
        val allGrantedFunction: ((Boolean) -> Unit)? = {
            if (it) {
                function?.let {
                    it()
                }
            } else {
                let {
                    //检查WIFI权限
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CHANGE_WIFI_STATE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        noPermissionDialog?.apply {
                            setTitle("权限提示")
                            setContent("权限已被禁止，请手动获得WiFi权限")
                            show()
                        }
                        return@let
                    }
                    //检查定位权限
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        noPermissionDialog?.apply {
                            setTitle("权限提示")
                            setContent("权限已被禁止，请手动获得定位权限")
                            show()
                        }
                        return@let
                    }
                    //检查读写权限
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        noPermissionDialog?.apply {
                            setTitle("权限提示")
                            setContent("权限已被禁止，请手动获得读写权限")
                            show()
                        }
                        return@let
                    }
                    function?.let {
                        it()
                    }
                }
            }
        }
        getPermission(allGrantedFunction)
    }

    private fun next() {
        val startConfigure: (() -> Unit)? = {
            next?.setLoading(true)
            if (latlng != null && !isTurn) {
                isTurn = true
                complete()
            }
        }
        checkPermission(startConfigure)
    }

    private fun complete() {
        Looper.myLooper()?.let {
            Handler(it).postDelayed({
                next?.setLoading(false)
                alphaAnimation?.cancel()
                listener?.complete(latlng)
            }, 500)
        }
    }

    fun viewShow(isShow: Boolean) {
        latlng = null
        if (isShow) {
            isTurn = false
            next?.setLoading(false)
            startLightAnimation()
            getLocation()
        } else {
            if (isViewShow) {
                next?.setLoading(false)
                alphaAnimation?.cancel()
                locationListener?.let {
                    mLocationManager?.removeUpdates(it)
                }
            }
        }
        isViewShow = isShow
    }

}

interface OnConnectFirstListener {
    fun complete(latlng: Latlng?)
}