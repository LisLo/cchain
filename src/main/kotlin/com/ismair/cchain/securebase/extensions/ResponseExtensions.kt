package com.ismair.cchain.securebase.extensions

import com.ismair.cchain.securebase.SecureBaseException
import com.ismair.cchain.securebase.SecureBaseResponse
import retrofit2.Response

fun <T> Response<SecureBaseResponse<T>>.extract(): SecureBaseResponse<T> {
    val body = body()
    when {
        body == null -> throw SecureBaseException("body is null")
        body.code != 0 -> throw SecureBaseException("${body.status}: ${body.message}")
        else -> return body
    }
}

fun <T> SecureBaseResponse<T>.extractObj(): T {
    if (data == null) {
        throw SecureBaseException("data is null")
    } else if (data.content == null) {
        throw SecureBaseException("content is null")
    } else if (data.content.isEmpty()) {
        throw SecureBaseException("content is empty and object was expected")
    } else {
        return data.content[0]
    }
}

fun <T> SecureBaseResponse<T>.extractList(): List<T> {
    if (data == null) {
        throw SecureBaseException("data is null")
    } else if (data.content == null) {
        throw SecureBaseException("content is null")
    } else {
        return if (data.content.isNotEmpty()) data.content else arrayListOf<T>()
    }
}

fun <T> Response<SecureBaseResponse<T>>.extractObj() = extract().extractObj()
fun <T> Response<SecureBaseResponse<T>>.extractList() = extract().extractList()