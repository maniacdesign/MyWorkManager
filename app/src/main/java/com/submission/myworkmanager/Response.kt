package com.submission.myworkmanager

import com.squareup.moshi.Json

data class Response(
    val int: Int,
    val name: String,
    @Json(name = "weather")
    val weatherList: List<Weather>,
    val main: Main,
)

data class Main(
    @Json(name = "temp")
    val temperature: Double
)

data class Weather(
    val main: String,
    val description: String
)
