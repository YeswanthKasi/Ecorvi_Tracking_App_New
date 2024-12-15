package com.ani.map

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UpdateChecker(private val context: Context) {

    private val owner = "YeswanthKasi"
    private val repo = "Ecorvi_Tracking_App_New"
    private val apiUrl = "https://api.github.com/"
    private val token = "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"

    private val retrofit = Retrofit.Builder()
        .baseUrl(apiUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(OkHttpClient.Builder().addInterceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        }.build())
        .build()

    private val service = retrofit.create(GitHubApiService::class.java)

    fun checkForUpdates(onUpdateAvailable: (String) -> Unit, onNoUpdate: () -> Unit) {
        val currentVersion = getAppVersionName()

        if (currentVersion == null) {
            Log.e("UpdateChecker", "Failed to get current version")
            onNoUpdate()
            return
        }

        service.getLatestRelease(owner, repo).enqueue(object : Callback<ReleaseResponse> {
            override fun onResponse(call: Call<ReleaseResponse>, response: Response<ReleaseResponse>) {
                if (response.isSuccessful) {
                    val release = response.body()
                    if (release != null) {
                        val latestVersion = release.tag_name.removePrefix("v")
                        val apkUrl = release.assets.firstOrNull()?.browser_download_url

                        Log.d("UpdateChecker", "Current: $currentVersion, Latest: $latestVersion, URL: $apkUrl")

                        if (apkUrl != null && isUpdateAvailable(currentVersion, latestVersion)) {
                            if (!hasAlreadyNotified(latestVersion)) {
                                onUpdateAvailable(apkUrl)
                                saveNotifiedVersion(latestVersion)
                            }
                        } else {
                            onNoUpdate()
                        }
                    } else {
                        onNoUpdate()
                    }
                } else {
                    Log.e("UpdateChecker", "GitHub API error: ${response.errorBody()?.string()}")
                    onNoUpdate()
                }
            }

            override fun onFailure(call: Call<ReleaseResponse>, t: Throwable) {
                Log.e("UpdateChecker", "Network error", t)
                onNoUpdate()
            }
        })
    }

    private fun getAppVersionName(): String? {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        val currentVersionParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val latestVersionParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(currentVersionParts.size, latestVersionParts.size)) {
            val currentPart = currentVersionParts.getOrNull(i) ?: 0
            val latestPart = latestVersionParts.getOrNull(i) ?: 0

            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }

    private fun hasAlreadyNotified(latestVersion: String): Boolean {
        val prefs = context.getSharedPreferences("update_checker", Context.MODE_PRIVATE)
        val lastNotifiedVersion = prefs.getString("last_notified_version", null)
        return lastNotifiedVersion == latestVersion
    }

    private fun saveNotifiedVersion(latestVersion: String) {
        val prefs = context.getSharedPreferences("update_checker", Context.MODE_PRIVATE)
        prefs.edit().putString("last_notified_version", latestVersion).apply()
    }
}
