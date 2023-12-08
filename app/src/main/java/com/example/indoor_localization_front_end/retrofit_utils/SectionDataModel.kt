package com.example.indoor_localization_front_end.retrofit_utils

import com.google.gson.annotations.SerializedName

data class SectionDataModel(
    @SerializedName("x") private val x: Double,
    @SerializedName("y") private val y: Double,
    @SerializedName("z") private val z: Double,
    @SerializedName("section") private val section: Int
)