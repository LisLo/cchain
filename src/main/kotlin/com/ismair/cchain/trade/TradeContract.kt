package com.ismair.cchain.trade

import com.ismair.cchain.contract.Contract
import com.ismair.cchain.trade.data.daxMap
import com.ismair.cchain.trade.extensions.forEachNonEqualPair
import com.ismair.cchain.trade.model.TradeConfirmation
import com.ismair.cchain.trade.model.TradeRejection
import com.ismair.cchain.trade.model.TradeRequest
import de.transbase.cchain.wrapper.TDBWrapper
import java.text.SimpleDateFormat
import java.util.*

class TradeContract : Contract() {
    override fun run(tdbWrapper: TDBWrapper) {
        println("loading responses ...")

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val responses = tdbWrapper.getParsedSentTransactions(listOf(TradeConfirmation::class, TradeRejection::class))
        val processedRequestIds = responses.map { it.document.requestId }.toMutableSet()
        val properties = responses
                .mapNotNull { it.document as? TradeConfirmation }
                .groupBy { Pair(it.request.name, it.request.isin) }
                .toList()
                .associate {
                    it.first to it.second.sumBy {
                        val shareCount = it.request.shareCount
                        if (it.request.mode == TradeRequest.Mode.BUY) shareCount else -shareCount
                    }
                }

        println("${processedRequestIds.size} responses found")

        while (true) {
            try {
                println("loading open requests ...")

                val openRequests = tdbWrapper
                        .getParsedReceivedTransactions<TradeRequest>()
                        .filter { !processedRequestIds.contains(it.id) }

                println("verifying ${openRequests.size} open requests ...")

                val validRequests = openRequests.mapNotNull {
                    val request = it.document
                    val property = properties[Pair(request.name, request.isin)]
                    val dateLimitParsed = try { sdf.parse(request.dateLimit) } catch (e: Exception) { null }

                    val message = when {
                        request.name.isEmpty() -> "name is required"
                        !daxMap.containsKey(request.isin) -> "isin is not valid"
                        request.shareCount <= 0 -> "share count has to be greater than zero"
                        request.priceLimit <= 0 -> "price limit has to be greater than zero"
                        dateLimitParsed == null -> "date limit could not be parsed"
                        dateLimitParsed.before(Date()) -> "request is expired"
                        request.mode == TradeRequest.Mode.SELL && (property == null || property < request.shareCount) -> "selling shares without property"
                        else -> null
                    }

                    if (message != null) {
                        println("rejecting request with id = ${it.id} with reason '$message' ...")

                        val rejection = TradeRejection(it.id, request, message)
                        tdbWrapper.createNewTransaction(it.chain, it.sender, rejection, true)
                        processedRequestIds.add(it.id)
                        null
                    } else {
                        it
                    }
                }

                println("search matching requests ...")

                val matchedRequestIds = mutableSetOf<Int>()
                validRequests.forEachNonEqualPair { transaction1, transaction2 ->
                    val id1 = transaction1.id
                    val id2 = transaction2.id
                    val request1 = transaction1.document
                    val request2 = transaction2.document

                    if (!matchedRequestIds.contains(id1) &&
                            !matchedRequestIds.contains(id2) &&
                            request1.mode == TradeRequest.Mode.BUY &&
                            request2.mode == TradeRequest.Mode.SELL &&
                            request1.isin == request2.isin &&
                            request1.shareCount == request2.shareCount &&
                            request1.priceLimit >= request2.priceLimit) {
                        val price = (request1.priceLimit + request2.priceLimit) / 2

                        println("confirming trades for ${request1.shareCount} shares of ${request1.isin} at price $price ...")

                        val confirmation1 = TradeConfirmation(id1, request1, price)
                        tdbWrapper.createNewTransaction("C-trade", transaction1.sender, confirmation1, true)
                        matchedRequestIds.add(id1)

                        val confirmation2 = TradeConfirmation(id2, request2, price)
                        tdbWrapper.createNewTransaction("C-trade", transaction2.sender, confirmation2, true)
                        matchedRequestIds.add(id2)
                    }
                }

                processedRequestIds.addAll(matchedRequestIds)
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting contract ...")
            }
        }
    }
}