package com.ismair.cchain.services

import com.ismair.cchain.extensions.shrink
import com.ismair.cchain.model.right.RightConfirmation

class AuthorizationService(list: List<RightConfirmation>) {
    private val requestIds = mutableSetOf<Int>()
    private val authorizedUsers = mutableSetOf<String>()

    init {
        list.forEach { add(it) }
    }

    fun add(confirmation: RightConfirmation) {
        val (requestId, user) = confirmation
        if (!requestIds.contains(requestId)) {
            authorizedUsers.add(user.shrink())
            requestIds.add(requestId)
        }
    }

    fun hasRight(user: String) = authorizedUsers.contains(user.shrink())
}