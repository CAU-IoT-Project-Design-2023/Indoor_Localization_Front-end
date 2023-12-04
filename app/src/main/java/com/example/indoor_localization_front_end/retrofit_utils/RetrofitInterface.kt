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

    @GET("get-sensor-data")
    fun sendSensorData(
        @Query("x") x: Double,
        @Query("y") y: Double,
        @Query("z") z: Double
    ): Call<String>

    @Multipart
    @POST("indoor-localization")
    suspend fun doIndoorLocalization(@Part sensorData: MultipartBody.Part): Response<LocalizationDataModel>
}