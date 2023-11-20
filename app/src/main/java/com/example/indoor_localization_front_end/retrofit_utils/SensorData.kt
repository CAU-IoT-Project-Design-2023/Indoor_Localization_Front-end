package com.example.indoor_localization_front_end.retrofit_utils

import com.google.gson.annotations.SerializedName

// 서버로 전송할 데이터
data class SensorData(
    @SerializedName("x") private val x: Double,
    @SerializedName("y") private val y: Double,
    @SerializedName("z") private val z: Double
)
