package com.example.xabarsos.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.xabarsos.model.MessageChannel
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)

class AppUpdateManager(private val context: Context) {

    companion object {
        const val CURRENT_VERSION_CODE = 1
        const val CURRENT_VERSION_NAME = "1.0"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    fun checkAppUpdate(serverBaseUrl: String, onUpdateAvailable: (AppVersionInfo) -> Unit) {
        scope.launch {
            try {
                val versionUrl = "$serverBaseUrl/api/app/version"
                val request = Request.Builder().url(versionUrl).get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val jsonObj = gson.fromJson(bodyStr, JsonObject::class.java)

                        val serverCode = if (jsonObj.has("versionCode") && !jsonObj.get("versionCode").isJsonNull) jsonObj.get("versionCode").asInt else CURRENT_VERSION_CODE
                        val serverName = if (jsonObj.has("versionName") && !jsonObj.get("versionName").isJsonNull) jsonObj.get("versionName").asString else "1.1"
                        val apkUrl = if (jsonObj.has("apkUrl") && !jsonObj.get("apkUrl").isJsonNull) jsonObj.get("apkUrl").asString else "$serverBaseUrl/download/apk"
                        val notes = if (jsonObj.has("releaseNotes") && !jsonObj.get("releaseNotes").isJsonNull) jsonObj.get("releaseNotes").asString else "Ilovaga yangi imkoniyatlar qo'shildi!"

                        if (serverCode > CURRENT_VERSION_CODE) {
                            val info = AppVersionInfo(serverCode, serverName, apkUrl, notes)
                            onUpdateAvailable(info)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Error checking version: ${e.message}")
            }
        }
    }

    fun downloadAndInstallApk(apkUrl: String, onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                _isDownloading.value = true
                _downloadProgress.value = 0.1f

                val request = Request.Builder().url(apkUrl).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _isDownloading.value = false
                        return@launch
                    }

                    val body = response.body ?: return@launch
                    val contentLength = body.contentLength()

                    val apkFile = File(context.cacheDir, "xabar_sos_update.apk")
                    if (apkFile.exists()) apkFile.delete()

                    var downloadedBytes = 0L
                    val inputStream: InputStream = body.byteStream()
                    val outputStream = FileOutputStream(apkFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (contentLength > 0) {
                            _downloadProgress.value = (downloadedBytes.toFloat() / contentLength.toFloat()).coerceIn(0.1f, 0.99f)
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    _downloadProgress.value = 1.0f
                    _isDownloading.value = false

                    // Trigger 1-Tap PackageInstaller Auto-Install
                    installApk(apkFile)
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Error downloading APK: ${e.message}")
                _isDownloading.value = false
            }
        }
    }

    private fun installApk(apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Error launching APK installer: ${e.message}")
        }
    }
}
