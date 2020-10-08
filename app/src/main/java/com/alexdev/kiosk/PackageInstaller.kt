package com.alexdev.kiosk

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URL


class MyPackageInstaller {
    companion object {
        fun checkPackageVersion(context: Context): Boolean {
            val text = URL("http://artmedia.ge/app/nasos_launcher").readText()
            val data = Json.parseJson(text)
            val url = data.jsonObject["update_url"].toString().replace("\"","")
            val version = data.jsonObject["version"].toString().replace("\"","")

            try {
                var appInfo: PackageInfo? = null
                try {
                    appInfo = context.packageManager.getPackageInfo("com.artmedia.nasosi", 0)
                } catch (e: java.lang.Exception) {
                }

                println(version)
                println(appInfo?.versionName)

                if (version != appInfo?.versionName) {
                    GlobalScope.launch {
                        installPackage(context, url)
                    }
                    return true
                }
            } catch (e: java.lang.Exception) {
                println(e)
                return false
            }

            return false
        }

        private suspend fun installPackage(context: Context, fileUrl: String) {
            try {
                println(fileUrl)
                val fileBuffer = URL(fileUrl).readBytes()
                println(fileBuffer.size)

                val pi = context.packageManager.packageInstaller
                val sessId: Int = pi.createSession(PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL))

                val session: PackageInstaller.Session = pi.openSession(sessId)

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

                val intent = (context as MainActivity).intent

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    0
                )
                session.commit(pendingIntent.intentSender)
                session.close()
            } catch (e: java.lang.Exception) {
                println(e)
            }
        }
    }
}
