package com.ismair.cchain.trade.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TradeExecution(
        val type: Type,
        val tradeOrderId: Int,
        val name: String,
        val isin: String,
        val shareCount: Int,
        val price: Int,
        val time: Date
) {
    enum class Type {
        CONFIRMATION, REJECTION
    }

    constructor(type: Type, tradeOrderId: Int, to: TradeOrder)
            : this(type, tradeOrderId, to.name, to.isin, to.shareCount, to.priceLimit, to.timeLimit)
}