package com.ani.map

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface IndusAppStoreService {
    @GET("apis/indus-developerdashboard-service/devtools/app/versions/{package}")
    fun getAppVersion(
        @Path("package") packageName: String,
        @Header("Authorization") apiKey: String
    ): Call<AppVersionResponse>
}

