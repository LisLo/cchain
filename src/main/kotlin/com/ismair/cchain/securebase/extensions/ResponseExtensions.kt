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
    } else if (data.count != 1 || data.content == null || data.content.size != 1) {
        throw SecureBaseException("object was expected")
    } else {
        return data.content[0]
    }
}

fun <T> SecureBaseResponse<T>.extractList(): List<T> {
    if (data == null) {
        throw SecureBaseException("data is null")
    } else if (data.count == 0) {
        return listOf()
    } else if (data.content == null || data.content.isEmpty()) {
        throw SecureBaseException("count is not zero but content is null or empty")
    } else {
        return data.content
    }
}

fun <T> Response<SecureBaseResponse<T>>.extractObj() = extract().extractObj()

fun <T> Response<SecureBaseResponse<T>>.extractList() = extract().extractList()