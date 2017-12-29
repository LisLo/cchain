package com.ismair.cchain.model

data class Booking(val transferId: Int, val chain: String, val sender: String, val receiver: String, val amount: Int, val purpose: String)