package com.ani.map

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AppVersionApi {
    @GET("get-app-version")
    fun getAppVersion(
        @Query("packageName") packageName: String,
        @Header("Authorization") authorization: String
    ): Call<AppVersionResponse>
}
