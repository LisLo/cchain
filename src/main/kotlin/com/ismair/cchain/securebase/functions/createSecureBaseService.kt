package com.ismair.cchain.securebase.functions

import com.google.gson.GsonBuilder
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun <T> createSecureBaseService(url: String, username: String, password: String, c: Class<T>) =
        Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(
                        GsonConverterFactory.create(
                                GsonBuilder()
                                        .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                                        .create()
                        )
                )
                .client(
                        OkHttpClient.Builder()
                                .addInterceptor {
                                    it.proceed(
                                            it.request().newBuilder()
                                                    .header("Authorization", Credentials.basic(username, password))
                                                    .build()
                                    )
                                }
                                .build()
                )
                .build()
                .create(c)!!