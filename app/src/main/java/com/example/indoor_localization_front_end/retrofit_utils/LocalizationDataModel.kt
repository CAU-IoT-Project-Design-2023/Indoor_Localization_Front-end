package com.example.indoor_localization_front_end.retrofit_utils

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

// 서버에서 받아올 JSON 형식의 데이터에 해당하는 class
data class LocalizationDataModel(
    @SerializedName("result")
    @Expose
    val result: String
)