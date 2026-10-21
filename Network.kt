package com.athaniqraq.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Network {
    val aladhan: AladhanApi by lazy {
        Retrofit.Builder().baseUrl("https://api.aladhan.com/")
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(AladhanApi::class.java)
    }
}
