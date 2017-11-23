package com.ismair.cchain.securebase

import com.google.gson.annotations.SerializedName

data class SecureBaseResponse<T>(
        val code: Int,
        val status: String,
        val message: String,
        val data: Data<T>?,
        val note: List<String>
) {
    class Data<T>(
            val server: String,
            val count: Int,
            @SerializedName(value = "services", alternate = arrayOf("connect","login","logout","new","open","close","info","modify","register","retrieve","fetch")) val content: List<T>?
    )
}