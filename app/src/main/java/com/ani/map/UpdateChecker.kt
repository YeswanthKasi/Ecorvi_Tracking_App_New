package com.ani.map

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UpdateChecker(private val context: Context) {
    // GitHub repository info
    private val owner = "YeswanthKasi"  // Your GitHub username
    private val repo = "Ecorvi_Tracking_App_New"  // Your GitHub repo name
    private val apiUrl = "https://api.github.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(apiUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(GitHubApiService::class.java)

    fun checkForUpdates(onUpdateAvailable: (String) -> Unit, onNoUpdate: () -> Unit) {
        val currentVersion = getAppVersionName()

        if (currentVersion == null) {
            // If unable to get the current version, skip checking
            onNoUpdate()
            return
        }

        // Make the network request to get the latest release from GitHub
        val call = service.getLatestRelease(owner, repo)
        call.enqueue(object : Callback<ReleaseResponse> {
            override fun onResponse(call: Call<ReleaseResponse>, response: Response<ReleaseResponse>) {
                if (response.isSuccessful) {
                    val release = response.body()
                    if (release != null) {
                        val latestVersion = release.tag_name.removePrefix("v") // Remove 'v' from version
                        val apkUrl = release.assets.firstOrNull()?.browser_download_url

                        if (apkUrl != null && isUpdateAvailable(currentVersion, latestVersion)) {
                            onUpdateAvailable(apkUrl)
                        } else {
                            onNoUpdate()
                        }
                    } else {
                        onNoUpdate()
                    }
                } else {
                    onNoUpdate()
                }
            }

            override fun onFailure(call: Call<ReleaseResponse>, t: Throwable) {
                t.printStackTrace()
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
        val currentVersionParts = currentVersion.split(".")
        val latestVersionParts = latestVersion.split(".")

        // Compare version parts one by one
        for (i in currentVersionParts.indices) {
            val currentPart = currentVersionParts.getOrNull(i)?.toIntOrNull() ?: 0
            val latestPart = latestVersionParts.getOrNull(i)?.toIntOrNull() ?: 0

            if (latestPart > currentPart) {
                return true  // New version is greater, update available
            } else if (latestPart < currentPart) {
                return false  // Current version is greater, no update
            }
        }
        return latestVersionParts.size > currentVersionParts.size // Handle cases where versions differ in length
    }
}
