package com.athaniqraq.app.data

import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PrayerRepository(private val api: AladhanApi = Network.aladhan) {
    suspend fun getTimes(lat: Double, lon: Double, maghribOffset: Int): List<PrayerTime> {
        val raw = api.timings(lat, lon, method = 7).data?.timings ?: error("No prayer timings returned")
        fun parse(key: String): LocalTime {
            val s = raw[key] ?: error("Missing timing: $key")
            val match = Regex("\\d{1,2}:\\d{2}").find(s)?.value ?: error("Invalid timing: $key")
            return LocalTime.parse(match, DateTimeFormatter.ofPattern("H:mm"))
        }
        fun fmt(t: LocalTime) = t.format(DateTimeFormatter.ofPattern("HH:mm"))
        val fajr = parse("Fajr")
        val sunrise = parse("Sunrise")
        val dhuhr = parse("Dhuhr")
        val asr = parse("Asr")
        // Iraq Shia practice: delay Maghrib until eastern redness has disappeared.
        val maghrib = parse("Maghrib").plusMinutes(maghribOffset.toLong())
        val isha = maghrib.plusMinutes(15)
        return listOf(
            PrayerTime("الفجر", fmt(fajr)), PrayerTime("الشروق", fmt(sunrise)),
            PrayerTime("الظهر", fmt(dhuhr)), PrayerTime("العصر", fmt(asr)),
            PrayerTime("المغرب", fmt(maghrib)), PrayerTime("العشاء", fmt(isha))
        )
    }
}
