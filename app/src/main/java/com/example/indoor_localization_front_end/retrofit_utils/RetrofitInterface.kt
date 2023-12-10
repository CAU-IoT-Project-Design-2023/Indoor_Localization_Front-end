package com.example.indoor_localization_front_end.retrofit_utils

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface RetrofitInterface {
    @GET("is-connected")
    fun isConnected(): Call<String>

    @Multipart
    @POST("save-sensor-data")
    suspend fun sendSensorData(@Part sensorData: MultipartBody.Part): Response<String>

    @GET("indoor-localization")
    suspend fun doIndoorLocalization(): Response<LocalizationDataModel>

    @POST("save-rssi-section-data")
    fun saveRssiAndSectionData(@Body rssiData: RssiData): Call<String>
}