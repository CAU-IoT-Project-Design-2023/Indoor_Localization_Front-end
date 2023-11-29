package com.example.indoor_localization_front_end.retrofit_utils

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // IP 주소
    private const val BASE_URL = "http://???.???.???:????"

    private fun getInstance(): Retrofit {
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun getApiService(): RetrofitInterface = getInstance().create(RetrofitInterface::class.java)
}