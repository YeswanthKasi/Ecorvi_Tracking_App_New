package com.ani.map

data class ReleaseResponse(
    val tag_name: String,          // e.g., "v1.2.3"
    val assets: List<ReleaseAsset> // List of assets related to the release (e.g., APK files)
)

data class ReleaseAsset(
    val browser_download_url: String // URL to download the APK file
)

