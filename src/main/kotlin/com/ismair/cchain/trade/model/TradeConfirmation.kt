package com.ismair.cchain.trade.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeConfirmation(
        override val requestId: Int,
        override val request: TradeRequest,
        val price: Int
) : TradeResponse