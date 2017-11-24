package com.ismair.cchain.model

import kotlinx.serialization.Serializable

@Serializable
data class Confirmation(val transferId: Int, val sender: String, val amount: Int, val purpose: String)