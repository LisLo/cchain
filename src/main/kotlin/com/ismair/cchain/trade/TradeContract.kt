package com.ismair.cchain.trade

import com.ismair.cchain.Contract
import com.ismair.cchain.trade.data.daxMap
import com.ismair.cchain.trade.extensions.forEachNonEqualPair
import com.ismair.cchain.trade.model.TradeExecution
import com.ismair.cchain.trade.model.TradeExecutionType
import com.ismair.cchain.trade.model.TradeOrder
import com.ismair.cchain.trade.model.TradeOrderType
import de.transbase.cchain.wrapper.TDBWrapper
import java.text.SimpleDateFormat
import java.util.*

class TradeContract(tdbWrapper: TDBWrapper) : Contract(tdbWrapper) {
    override fun run() {
        println("loading executions ...")

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val executions = tdbWrapper.getParsedSentTransactions<TradeExecution>()
        val processedOrderIds = executions.map { it.document.orderId }.toMutableSet()
        val properties = executions
                .filter { it.document.executionType == TradeExecutionType.CONFIRMATION }
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
                    val dateLimitParsed = try { sdf.parse(order.dateLimit) } catch (e: Exception) { null }

                    val message = when {
                        order.name.isEmpty() -> "name is required"
                        !daxMap.containsKey(order.isin) -> "isin is not valid"
                        order.shareCount <= 0 -> "share count has to be greater than zero"
                        order.priceLimit <= 0 -> "price limit has to be greater than zero"
                        dateLimitParsed == null -> "date limit could not be parsed"
                        dateLimitParsed.before(Date()) -> "order is expired"
                        order.orderType == TradeOrderType.SELL && (property == null || property < order.shareCount) -> "selling shares without property"
                        else -> null
                    }

                    if (message != null) {
                        println("rejecting order with id = ${it.id} with reason '$message' ...")

                        val rejection = TradeExecution.createRejection(it.id, order, sdf.format(Date()), message)
                        tdbWrapper.createNewTransaction(it.chain, it.sender, rejection, true)
                        null
                    } else {
                        it
                    }
                }

                println("search matching orders ...")

                val matchedOrderIds = mutableSetOf<Int>()
                validOrders.forEachNonEqualPair { transaction1, transaction2 ->
                    val id1 = transaction1.id
                    val id2 = transaction2.id
                    val order1 = transaction1.document
                    val order2 = transaction2.document

                    if (!matchedOrderIds.contains(id1) &&
                            !matchedOrderIds.contains(id2) &&
                            order1.orderType == TradeOrderType.BUY &&
                            order2.orderType == TradeOrderType.SELL &&
                            order1.isin == order2.isin &&
                            order1.shareCount == order2.shareCount &&
                            order1.priceLimit >= order2.priceLimit) {
                        val price = (order1.priceLimit + order2.priceLimit) / 2
                        val date = sdf.format(Date())

                        println("confirming trades for ${order1.shareCount} shares of ${order1.isin} at price $price ...")

                        val confirmation1 = TradeExecution.createConfirmation(id1, order1, price, date)
                        tdbWrapper.createNewTransaction("C-trade", transaction1.sender, confirmation1, true)
                        matchedOrderIds.add(id1)

                        val confirmation2 = TradeExecution.createConfirmation(id2, order2, price, date)
                        tdbWrapper.createNewTransaction("C-trade", transaction2.sender, confirmation2, true)
                        matchedOrderIds.add(id2)
                    }
                }

                processedOrderIds.addAll(matchedOrderIds)
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting contract ...")
            }
        }
    }
}