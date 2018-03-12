package com.ismair.cchain.cash.model

import kotlinx.serialization.Serializable

@Serializable
data class Confirmation(val transferId: Int, val user: String, val amount: Int, val purpose: String)