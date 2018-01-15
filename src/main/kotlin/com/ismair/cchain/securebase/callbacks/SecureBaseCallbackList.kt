package com.ismair.cchain.securebase.callbacks

import com.ismair.cchain.securebase.SecureBaseResponse

abstract class SecureBaseCallbackList<T>() : SecureBaseCallback<T>() {
    abstract fun onSuccess(content: List<T>)

    override fun onSuccess(response: SecureBaseResponse<T>) {
        if (response.data == null) {
            onError("data is null")
        } else if (response.data.count == 0) {
            onSuccess(listOf())
        } else if (response.data.content == null || response.data.content.isEmpty()) {
            onError("count is not zero but content is null or empty")
        } else {
            onSuccess(response.data.content)
        }
    }
}