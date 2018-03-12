package com.ismair.cchain.trade

import com.ismair.cchain.Contract
import com.ismair.cchain.trade.model.TradeExecution
import com.ismair.cchain.trade.model.TradeOrder
import de.transbase.cchain.wrapper.TDBWrapper
import java.util.*

class TradeContract(tdbWrapper: TDBWrapper, private val daxMap: Map<String, String>) : Contract(tdbWrapper) {
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

                        val tradeExecution = TradeExecution(TradeExecution.Type.REJECTION, it.id, tradeOrder)
                        tdbWrapper.createNewTransaction(it.chain, it.sender, tradeExecution, true)
                        null
                    } else {
                        it
                    }
                }
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting contract ...")
            }
        }
    }
}