package com.alexdev.kiosk

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyManager.LOCK_TASK_FEATURE_HOME
import android.app.admin.DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS
import android.app.admin.SystemUpdatePolicy
import android.content.*
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.ACTION_SESSION_COMMITTED
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.URL


class MainActivity : AppCompatActivity() {
    private lateinit var mAdminComponentName: ComponentName
    private lateinit var mDevicePolicyManager: DevicePolicyManager

    private var kioskEnabled : Boolean = false;

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

        val exitBT = findViewById<Button>(R.id.exit_button)

        exitBT.setOnClickListener {
            if (isAdmin()) {
                kioskEnabled = !kioskEnabled

                setKioskPolicies(kioskEnabled)

                exitBT.text = if(kioskEnabled) "გამორთვა" else "ჩართვა"

            }
        }

        batteryLevel();

        addHomeButton()

//        Thread(Runnable {
//            try {
//                test.installPackage(this,"http://192.168.43.18/tmp/app-release.apk")
//            } catch (ex: Exception) {
//                ex.printStackTrace()
//            }
//        }).start()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mDevicePolicyManager.setLockTaskFeatures(mAdminComponentName, LOCK_TASK_FEATURE_HOME or LOCK_TASK_FEATURE_NOTIFICATIONS)
        };
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
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, false)
         setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, false)
        setUserRestriction(UserManager.DISALLOW_CONFIG_TETHERING, disallow)
       // mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, disallow)
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

    private fun addHomeButton(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }


        val mLauncherHeader = LayoutInflater.from(this).inflate(R.layout.home_button, null)

        val LAYOUT_FLAG: Int
        LAYOUT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )


        params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM

        params.height = 100
        params.width = 130

        val mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
       mWindowManager.addView(mLauncherHeader, params)

        val homeBT = mLauncherHeader.findViewById<ImageButton>(R.id.home_bt)

        homeBT.setOnClickListener {
            startActivity(intent)
        }
    }
}



class test {
    companion object {
    fun installPackage(context: Context,fileUrl:String) {
        val pi = context.packageManager.packageInstaller
        val sessId: Int = pi.createSession(SessionParams(SessionParams.MODE_FULL_INSTALL))

        val session: PackageInstaller.Session = pi.openSession(sessId)

        val fileBuffer = URL(fileUrl).readBytes()
        var sizeBytes: Long = fileBuffer.size.toLong()

        var inputStream = fileBuffer.inputStream()
        var out = session.openWrite("my_app_session", 0, sizeBytes)

        var total = 0
        val buffer = ByteArray(65536)
        var c: Int

        while (inputStream.read(buffer).also { c = it } != -1) {
            total += c
            out.write(buffer, 0, c)
        }

        session.fsync(out)
        inputStream.close()
        out.close()

        println("InstallApkViaPackageInstaller - Success: streamed apk $total bytes")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessId,
            Intent(ACTION_SESSION_COMMITTED),
            0
        )
       session.commit(pendingIntent.intentSender)
       session.close()
    }
    }
}



