package com.ismair.cchain.trade

import com.ismair.cchain.Contract
import com.ismair.cchain.trade.model.TradeExecution
import com.ismair.cchain.trade.model.TradeOrder
import de.transbase.cchain.wrapper.TDBWrapper
import java.util.*

class TradeContract(tdbWrapper: TDBWrapper) : Contract(tdbWrapper) {
    override fun run() {
        println("loading executions ...")

        val executions = tdbWrapper.getParsedSentTransactions<TradeExecution>()
        val processedTradeOrderIds = executions.map { it.document.tradeOrderId }.toMutableSet()
        val countConfirmations = executions.count { it.document.type == TradeExecution.Type.CONFIRMATION }
        val countRejections = executions.count { it.document.type == TradeExecution.Type.REJECTION }

        println("${processedTradeOrderIds.size} executions found ($countConfirmations confirmations, $countRejections rejections)")

        while (true) {
            try {
                println("loading open trade orders ...")

                val openTradeOrders = tdbWrapper.getParsedReceivedTransactions<TradeOrder>().filter { !processedTradeOrderIds.contains(it.id) }

                println("verifying ${openTradeOrders.size} open trade orders ...")

                val validTradeOrders = openTradeOrders.mapNotNull {
                    val tradeOrder = it.document
                    if (tradeOrder.name.isEmpty() ||
                            !daxMap.containsKey(tradeOrder.isin) ||
                            tradeOrder.shareCount <= 0 ||
                            tradeOrder.priceLimit <= 0 ||
                            tradeOrder.timeLimit.before(Date())) {
                        println("trade order with id = ${it.id} is not valid, rejecting it ...")

                        val rejection = TradeExecution(TradeExecution.Type.REJECTION, it.id, tradeOrder)
                        tdbWrapper.createNewTransaction(it.chain, it.sender, rejection, true)
                        null
                    } else {
                        it
                    }
                }

                println("search matching trade orders ...")

                validTradeOrders.filter { it.document.type == TradeOrder.Type.BUY }.forEach {
                    val buyTradeOrder = it.document
                    val sellTradeOrder = validTradeOrders.firstOrNull {
                        val sellTradeOrder = it.document
                        sellTradeOrder.type == TradeOrder.Type.SELL &&
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

                        val confirmation1 = TradeExecution(TradeExecution.Type.CONFIRMATION, 0, buyTradeOrder.name, isin, shareCount, price, date)
                        tdbWrapper.createNewTransaction("C-trade", "", confirmation1, true)

                        val confirmation2 = TradeExecution(TradeExecution.Type.CONFIRMATION, 0, sellTradeOrder.name, isin, shareCount, price, date)
                        tdbWrapper.createNewTransaction("C-trade", "", confirmation2, true)
                    }
                }
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting contract ...")
            }
        }
    }
}