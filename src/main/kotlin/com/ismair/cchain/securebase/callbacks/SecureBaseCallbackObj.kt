package com.ismair.cchain.securebase.callbacks

import com.ismair.cchain.securebase.SecureBaseResponse

abstract class SecureBaseCallbackObj<T>() : SecureBaseCallback<T>() {
    abstract fun onSuccess(content: T)

    override fun onSuccess(response: SecureBaseResponse<T>) {
        if (response.data == null) {
            onError("data is null")
        } else if (response.data.content == null) {
            onError("content is null")
        } else if (response.data.content.isEmpty()) {
            onError("content is empty and object was expected")
        } else {
            onSuccess(response.data.content[0])
        }
    }
}