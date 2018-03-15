package com.ismair.cchain.trade

import com.ismair.cchain.Contract
import com.ismair.cchain.trade.model.TradeExecution
import com.ismair.cchain.trade.model.TradeExecutionType
import com.ismair.cchain.trade.model.TradeOrder
import com.ismair.cchain.trade.model.TradeOrderType
import de.transbase.cchain.wrapper.TDBWrapper
import java.util.*

class TradeContract(tdbWrapper: TDBWrapper) : Contract(tdbWrapper) {
    override fun run() {
        println("loading executions ...")

        val executions = tdbWrapper.getParsedSentTransactions<TradeExecution>()
        val processedOrderIds = executions.map { it.document.orderId }
        val properties = executions
                .groupBy { Pair(it.document.name, it.document.isin) }
                .toList()
                .associate {
                    it.first to it.second.sumBy {
                        val doc = it.document
                        if (doc.orderType == TradeOrderType.BUY) doc.shareCount else -doc.shareCount
                    }
                }

        println("${processedOrderIds.size} executions found")

        while (true) {
            try {
                println("loading open orders ...")

                val openOrders = tdbWrapper.getParsedReceivedTransactions<TradeOrder>().filter { !processedOrderIds.contains(it.id) }

                println("verifying ${openOrders.size} open orders ...")

                val validOrders = openOrders.mapNotNull {
                    val order = it.document
                    val property = properties[Pair(order.name, order.isin)]

                    val message = when {
                        order.name.isEmpty() -> "name is required"
                        !daxMap.containsKey(order.isin) -> "isin is not valid"
                        order.shareCount <= 0 -> "share count has to be greater than zero"
                        order.priceLimit <= 0 -> "price limit has to be greater than zero"
                        order.timeLimit.before(Date()) -> "order is expired"
                        order.orderType == TradeOrderType.SELL && (property == null || property < order.shareCount) -> "selling shares without property"
                        else -> null
                    }

                    if (message != null) {
                        println("rejecting order with id = ${it.id} with reason '$message' ...")

                        val rejection = TradeExecution(TradeExecutionType.REJECTION, it.id, order, message)
                        tdbWrapper.createNewTransaction(it.chain, it.sender, rejection, true)
                        null
                    } else {
                        it
                    }
                }

                println("search matching orders ...")

                validOrders.filter { it.document.orderType == TradeOrderType.BUY }.forEach {
                    val buyTradeOrder = it.document
                    val sellTradeOrder = validOrders.firstOrNull {
                        val sellTradeOrder = it.document
                        sellTradeOrder.orderType == TradeOrderType.SELL &&
                                sellTradeOrder.isin == buyTradeOrder.isin &&
                                sellTradeOrder.shareCount == buyTradeOrder.shareCount &&
                                sellTradeOrder.priceLimit <= buyTradeOrder.priceLimit
                    }?.document

                    if (sellTradeOrder != null) {
                        println("found matching trades")

                        val isin = buyTradeOrder.isin
                        val shareCount = buyTradeOrder.shareCount
                        val price = (buyTradeOrder.priceLimit + sellTradeOrder.priceLimit) / 2
                        val date = Date()

                        val confirmation1 = TradeExecution(TradeExecutionType.CONFIRMATION, 0, TradeOrderType.BUY, buyTradeOrder.name, isin, shareCount, price, date, "success")
                        tdbWrapper.createNewTransaction("C-trade", "", confirmation1, true)

                        val confirmation2 = TradeExecution(TradeExecutionType.CONFIRMATION, 0, TradeOrderType.SELL, sellTradeOrder.name, isin, shareCount, price, date, "success")
                        tdbWrapper.createNewTransaction("C-trade", "", confirmation2, true)
                    }
                }
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting contract ...")
            }
        }
    }
}