package com.athaniqraq.app.data

data class PrayerTime(val name: String, val time: String, val minutesFromMidnight: Int = 0)
data class Dua(val id: String, val title: String, val text: String, val note: String = "")
data class PrayerApiResponse(val code: Int, val data: PrayerApiData?)
data class PrayerApiData(val timings: Map<String, String>?)
