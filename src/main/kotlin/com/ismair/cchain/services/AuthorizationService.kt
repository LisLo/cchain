package com.ismair.cchain.services

import com.ismair.cchain.model.right.RightConfirmation

class AuthorizationService(list: List<RightConfirmation>) {
    private val requestIds = mutableSetOf<Int>()
    private val authorizedUsers = mutableSetOf<String>()

    init {
        list.forEach { add(it) }
    }

    fun add(confirmation: RightConfirmation) {
        if (requestIds.contains(confirmation.requestId)) {
            authorizedUsers.add(confirmation.user)
        }
    }

    fun hasRight(user: String) = authorizedUsers.contains(user)
}