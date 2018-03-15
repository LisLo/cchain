package com.ismair.cchain.trade.model

import kotlinx.serialization.Serializable

@Serializable
data class TradeRequest(
        val mode: TradeRequestMode,
        val name: String,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
)