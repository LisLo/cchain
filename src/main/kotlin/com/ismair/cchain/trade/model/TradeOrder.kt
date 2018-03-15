package com.ismair.cchain.trade.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TradeOrder(
        val orderType: TradeOrderType,
        val name: String,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
)