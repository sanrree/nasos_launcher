package com.alexdev.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.*
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

open class OwnerActivity : AppCompatActivity(){
    protected lateinit var mAdminComponentName: ComponentName
    protected lateinit var mDevicePolicyManager: DevicePolicyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAdminComponentName = AdminReceiver.getComponentName(this)
        mDevicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    protected fun isAdmin() = mDevicePolicyManager.isDeviceOwnerApp(packageName)

    protected fun setKioskPolicies(enable: Boolean,allowedPackages:Array<String>) {
        setRestrictions(enable)
        setAsHomeApp(enable)
        setKeyGuardEnabled(enable)
        setLockTask(enable,allowedPackages)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mDevicePolicyManager.setLockTaskFeatures(mAdminComponentName, DevicePolicyManager.LOCK_TASK_FEATURE_HOME or DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS)
        };
    }

    private fun setRestrictions(disallow: Boolean) {
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, disallow)
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, disallow)
        setUserRestriction(UserManager.DISALLOW_ADD_USER, disallow)
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, false)
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, false)
        setUserRestriction(UserManager.DISALLOW_CONFIG_TETHERING, disallow)
    }

    private fun setUserRestriction(restriction: String, disallow: Boolean) = if (disallow) {
        mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction)
    } else {
        mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction)
    }

    private fun setLockTask(start: Boolean,allowedPackages:Array<String>) {
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

    override fun onBackPressed() {
        return
    }
}