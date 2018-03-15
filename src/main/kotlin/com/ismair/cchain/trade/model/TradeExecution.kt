package com.ismair.cchain.trade.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TradeExecution(
        val executionType: TradeExecutionType,
        val orderId: Int,
        val orderType: TradeOrderType,
        val name: String,
        val isin: String,
        val shareCount: Int,
        val price: Int,
        val time: Date,
        val message: String?
) {
    companion object {
        fun createRejection(orderId: Int, order: TradeOrder, message: String) = TradeExecution(
                TradeExecutionType.REJECTION,
                orderId,
                order.orderType,
                order.name,
                order.isin,
                order.shareCount,
                order.priceLimit,
                order.timeLimit,
                message
        )

        fun createConfirmation(orderId: Int, order: TradeOrder, price: Int, time: Date) = TradeExecution(
                TradeExecutionType.CONFIRMATION,
                orderId,
                order.orderType,
                order.name,
                order.isin,
                order.shareCount,
                price,
                time,
                null
        )
    }
}