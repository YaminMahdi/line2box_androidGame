package com.diu.yk_games.line2box.model


import com.google.gson.annotations.SerializedName

data class CountryInfo(
    @SerializedName("city")
    val city: String = "",
    @SerializedName("country")
    val country: String = "",
    @SerializedName("query")
    val query: String = ""
)