package com.example.indoor_localization_front_end.retrofit_utils

import com.google.gson.annotations.SerializedName

data class RssiData(
    @SerializedName("s1") private val s1: String,
    @SerializedName("r1") private val r1: Int,
    @SerializedName("s2") private val s2: String,
    @SerializedName("r2") private val r2: Int,
    @SerializedName("s3") private val s3: String,
    @SerializedName("r3") private val r3: Int,
    @SerializedName("section") private val section: Int
)
