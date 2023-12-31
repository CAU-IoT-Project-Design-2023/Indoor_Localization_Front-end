package com.example.indoor_localization_front_end.retrofit_utils

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RetrofitInterface {
    @GET("is-connected")
    fun isConnected(): Call<String>

    @Multipart
    @POST("save-sensor-data")
    suspend fun sendSensorData(@Part sensorData: MultipartBody.Part): Response<String>

    @GET("rssi-measure")
    suspend fun doLocalization(): Response<String>
}