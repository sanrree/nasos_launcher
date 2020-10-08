package com.alexdev.kiosk

import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MainActivity : OwnerActivity() {
    private var kioskEnabled : Boolean = false;

    val allowedPackages = arrayOf(
        "com.artmedia.nasosi",
        "com.google.android.apps.maps",
        "com.android.vending",
        "com.viber.voip",
        "com.android.settings",
        "com.whatsapp",
        "ru.yandex.yandexnavi",
        "com.vgc.volumeandbrightnesscontrol"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initGridView()
        initNetworkButton()
        initRefreshButton()
        initToggleButton()
        initBatteryIndicator()

        startHeaderService()

        checkUpdates()
    }

    private fun startHeaderService(){
        println("start service")

        val headerServiceIntent = Intent(this,HeaderService::class.java)
        startService(headerServiceIntent)
    }

    private fun checkUpdates() {
        GlobalScope.launch {
            try {
                val shouldUpdate = MyPackageInstaller.checkPackageVersion(this@MainActivity)
                println(shouldUpdate)

                if (shouldUpdate) {
                    runOnUiThread {
                        val progressDialog = Dialog(this@MainActivity)
                        progressDialog.setContentView(R.layout.progress_layout)
                        progressDialog.setCancelable(false)
                        progressDialog.show()
                    }
                }
            } catch (ex: Exception) {
                println(ex.message)
                ex.printStackTrace()
            }
        }
    }

    private fun initToggleButton(){
        val toggleLockTaskButton = findViewById<Button>(R.id.toggle_lock_task_mode_bt)

        toggleLockTaskButton.setOnClickListener {
            if (isAdmin()) {
                kioskEnabled = !kioskEnabled

                //setKioskPolicies(kioskEnabled, allowedPackages)

                toggleLockTaskButton.text = if (kioskEnabled) "გამორთვა" else "ჩართვა"


                val intent = Intent("com.alexdev.kiosk.toggle_header_service")
                intent.putExtra("toggle", kioskEnabled)
                sendBroadcast(intent)

            }
        }
    }

    private fun initRefreshButton(){
        val refreshButton = findViewById<ImageButton>(R.id.refreshBT)

        refreshButton.setOnClickListener {
            finish()
            startActivity(intent)
        }
    }

    private fun initNetworkButton() {
        val networkButton = findViewById<ImageButton>(R.id.network_bt)

        networkButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivity(intent)
        }
    }

    private fun initGridView(){

        val gridView = findViewById<GridView>(R.id.gridView)

        val apps: MutableList<AppView> = mutableListOf<AppView>()

        allowedPackages.forEach {
            try {
                val icon: Drawable = packageManager.getApplicationIcon(it)
                val name: String = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(
                        it,
                        GET_META_DATA
                    )
                ).toString()

                apps.add(AppView(name, icon, it))


            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        gridView.adapter = MyGridAdapter(this, apps.toTypedArray())

        gridView.setOnItemClickListener { _, _, i, _ ->
            run {
                apps[i]?.let {
                    val launchIntent = packageManager.getLaunchIntentForPackage(it.packageName);
                    launchIntent?.let { intent ->
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun initBatteryIndicator() {
        val batteryLevelReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val rawlevel = intent.getIntExtra("level", -1)
                val scale = intent.getIntExtra("scale", -1)
                var level = -1
                if (rawlevel >= 0 && scale > 0) {
                    level = rawlevel * 100 / scale
                }
                findViewById<TextView>(R.id.batteryPercent).text = "$level%"
                val batteryLevelProgress = findViewById<ProgressBar>(R.id.batteryProgress)
                batteryLevelProgress.progress = level

                val charger = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                val plugged = charger == BatteryManager.BATTERY_PLUGGED_AC ||
                        charger == BatteryManager.BATTERY_PLUGGED_USB ||
                        charger == BatteryManager.BATTERY_PLUGGED_WIRELESS;

                val indicatorColor = getColor(if (plugged) R.color.colorAccentDark else R.color.notCharingColor)

                batteryLevelProgress.setBackgroundColor(indicatorColor)

            }
        }
        val batteryLevelFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryLevelReceiver, batteryLevelFilter)
    }
}
