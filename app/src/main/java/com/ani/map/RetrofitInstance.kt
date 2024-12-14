package com.ani.map

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://github.com/YeswanthKasi/Ecorvi_Tracking_App_New/releases/" // Replace with your API base URL

    val service: AppVersionApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AppVersionApi::class.java)
    }
}
