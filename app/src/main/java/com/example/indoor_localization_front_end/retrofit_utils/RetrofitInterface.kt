package com.example.indoor_localization_front_end.retrofit_utils

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    @POST("indoor-localization")
    suspend fun doIndoorLocalization(@Body sensorData: SensorData): Response<LocalizationDataModel>
}