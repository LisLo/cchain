package com.ismair.cchain.model

import kotlinx.serialization.Serializable

@Serializable
data class Transfer(val receiver: String, val amount: Int, val purpose: String)