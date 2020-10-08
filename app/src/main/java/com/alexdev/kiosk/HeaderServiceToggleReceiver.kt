package com.alexdev.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK

class HeaderServiceToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.extras?.getBoolean("toggle")?.also {
            val intent = Intent(context, HeaderService::class.java)
            intent.putExtra("toggle", it)
            context?.startService(intent)
        }
    }
}