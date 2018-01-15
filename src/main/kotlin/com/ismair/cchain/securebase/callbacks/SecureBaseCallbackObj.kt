package com.ismair.cchain.securebase.callbacks

import com.ismair.cchain.securebase.SecureBaseResponse

abstract class SecureBaseCallbackObj<T>() : SecureBaseCallback<T>() {
    abstract fun onSuccess(content: T)

    override fun onSuccess(response: SecureBaseResponse<T>) {
        if (response.data == null) {
            onError("data is null")
        } else if (response.data.count != 1 || response.data.content == null || response.data.content.size != 1) {
            onError("object was expected")
        } else {
            onSuccess(response.data.content[0])
        }
    }
}