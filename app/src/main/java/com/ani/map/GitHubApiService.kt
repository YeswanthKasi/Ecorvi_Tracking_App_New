package com.ani.map

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubApiService {

    // Endpoint to get the latest release details from a GitHub repo
    @GET("repos/{owner}/{repo}/releases/latest")
    fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<ReleaseResponse>
}
