package com.ismair.cchain.securebase

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class SecureBaseCallback<T> : Callback<SecureBaseResponse<T>> {
    abstract fun onSuccess(response: SecureBaseResponse<T>)
    abstract fun onError(message: String)

    override fun onResponse(call: Call<SecureBaseResponse<T>>?, response: Response<SecureBaseResponse<T>>?) {
        if (response == null) {
            onError("response is null")
        } else {
            val body = response.body()
            when {
                body == null -> onError("body is null")
                body.code != 0 -> onError("${body.status}: ${body.message}")
                else -> onSuccess(body)
            }
        }
    }

    override fun onFailure(call: Call<SecureBaseResponse<T>>?, t: Throwable?) {
        onError(if (t != null) "$t.cause - $t.message" else "unknown error")
    }
}