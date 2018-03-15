package com.ismair.cchain.trade.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeRejection(
        override val requestId: Int,
        override val request: TradeRequest,
        val message: String
) : TradeResponse