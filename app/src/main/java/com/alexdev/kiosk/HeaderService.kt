package com.alexdev.kiosk

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import java.lang.Exception


class HeaderService : Service() {
    var mWindowManager: WindowManager? = null
    var mLauncherHeader: View? = null

    var broadcastReceiver: HeaderServiceToggleReceiver? = null

    override fun onCreate() {
        super.onCreate()

        toggleHeaderService(true)

        val filter = IntentFilter("com.alexdev.kiosk.toggle_header_service")
        broadcastReceiver = HeaderServiceToggleReceiver()
        registerReceiver(broadcastReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.extras?.getBoolean("toggle")?.also {
            toggleHeaderService(it)
        }

        return START_STICKY
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }

    private fun toggleHeaderService(toggle: Boolean) {
        requestPermission()

        if (toggle) {
            showHeader()
        } else {
            println(mLauncherHeader)
            hideHeader()
        }
    }

    private fun hideHeader() {

        try {
            mWindowManager?.removeView(mLauncherHeader)
        }catch (e:Exception){
            println("message")
        }
    }

    private fun showHeader() {
        if(mLauncherHeader?.windowToken != null){
            return
        }

        mLauncherHeader = LayoutInflater.from(this).inflate(R.layout.home_button, null)
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM

        params.height = 100
        params.width = 130

        mWindowManager?.addView(mLauncherHeader, params)

        val homeBT = mLauncherHeader?.findViewById<ImageButton>(R.id.home_bt)

        homeBT?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK;
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideHeader()
        unregisterReceiver(broadcastReceiver)
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}