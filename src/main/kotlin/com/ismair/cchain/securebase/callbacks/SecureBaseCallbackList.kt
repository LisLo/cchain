package com.ismair.cchain.securebase.callbacks

import com.ismair.cchain.securebase.SecureBaseResponse

abstract class SecureBaseCallbackList<T>() : SecureBaseCallback<T>() {
    abstract fun onSuccess(content: List<T>)

    override fun onSuccess(response: SecureBaseResponse<T>) {
        if (response.data == null) {
            onError("data is null")
        } else if (response.data.content == null) {
            onError("content is null")
        } else if (response.data.content.isEmpty()) {
            onSuccess(arrayListOf<T>())
        } else {
            onSuccess(response.data.content)
        }
    }
}