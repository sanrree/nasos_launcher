package com.alexdev.kiosk

import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.graphics.drawable.Drawable
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var mAdminComponentName: ComponentName
    private lateinit var mDevicePolicyManager: DevicePolicyManager

    private val KIOSK_PACKAGE: String = "com.alexdev.kiosk"

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main)

        mAdminComponentName = AdminReceiver.getComponentName(this)
        mDevicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        mDevicePolicyManager.removeActiveAdmin(mAdminComponentName)

        val gridView = findViewById<GridView>(R.id.gridView)

        val apps: MutableList<AppView> = mutableListOf<AppView>()

        allowedPackages.forEach {
            addApp(it)?.let { appView ->
                apps.add(appView);
            }
        }

        gridView.setAdapter(MyGridAdapter(this, apps.toTypedArray()))

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

        val imageButton = findViewById<ImageButton>(R.id.mobileDataToggle)

        imageButton.setOnClickListener {
            setMobileDataEnabled()
        }

        val refreshBT = findViewById<ImageButton>(R.id.refreshBT)

        refreshBT.setOnClickListener {
            finish()
            startActivity(intent)
        }

        batteryLevel();

        if (isAdmin()) {
            setKioskPolicies(true)
        }
    }

    private fun addApp(packageName: String): AppView? {
        try {
            val icon: Drawable = packageManager.getApplicationIcon(packageName)
            val name: String = packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(
                    packageName,
                    GET_META_DATA
                )
            ).toString()

            return AppView(name, icon, packageName);

        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return null;
    }

    private fun isAdmin() = mDevicePolicyManager.isDeviceOwnerApp(packageName)

    fun setMobileDataEnabled() {
        val intent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent)
    }

    private fun setKioskPolicies(enable: Boolean) {
        setRestrictions(enable)
        //setUpdatePolicy(enable)
        setAsHomeApp(enable)
        setKeyGuardEnabled(enable)
        setLockTask(enable)
        //setImmersiveMode(enable)
    }

    private fun batteryLevel() {
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
                val plugged =
                    charger == BatteryManager.BATTERY_PLUGGED_AC ||
                            charger == BatteryManager.BATTERY_PLUGGED_USB ||
                            charger == BatteryManager.BATTERY_PLUGGED_WIRELESS;

                Log.i("AAAAA", plugged.toString());

                batteryLevelProgress.setBackgroundColor(getColor(if (plugged) R.color.colorAccentDark else R.color.notCharingColor));

            }
        }
        val batteryLevelFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryLevelReceiver, batteryLevelFilter)
    }

    private fun setRestrictions(disallow: Boolean) {
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, disallow)
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, disallow)
        setUserRestriction(UserManager.DISALLOW_ADD_USER, disallow)
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, disallow)
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, disallow)
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, disallow)
    }

    private fun setUserRestriction(restriction: String, disallow: Boolean) = if (disallow) {
        mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction)
    } else {
        mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction)
    }

    private fun setLockTask(start: Boolean) {
        val list: MutableList<String> = allowedPackages.toMutableList()
        list.add(packageName)

        mDevicePolicyManager.setLockTaskPackages(
            mAdminComponentName, if (start) list.toTypedArray() else arrayOf()
        )

        if (start) {
            startLockTask()
        } else {
            stopLockTask()
        }
    }

    private fun setUpdatePolicy(enable: Boolean) {
        if (enable) {
            mDevicePolicyManager.setSystemUpdatePolicy(
                mAdminComponentName,
                SystemUpdatePolicy.createWindowedInstallPolicy(60, 120)
            )
        } else {
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName, null)
        }
    }

    private fun setAsHomeApp(enable: Boolean) {
        if (enable) {
            val intentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            mDevicePolicyManager.addPersistentPreferredActivity(
                mAdminComponentName,
                intentFilter,
                ComponentName(packageName, MainActivity::class.java.name)
            )
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                mAdminComponentName, packageName
            )
        }
    }

    private fun setKeyGuardEnabled(enable: Boolean) {
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, !enable)
    }

    private fun setImmersiveMode(enable: Boolean) {
        if (enable) {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.decorView.systemUiVisibility = flags
        } else {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            window.decorView.systemUiVisibility = flags
        }
    }

    override fun onBackPressed() {
        return
    }
}
