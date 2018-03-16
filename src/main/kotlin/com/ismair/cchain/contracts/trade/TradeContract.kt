package com.ismair.cchain.contracts.trade

import com.ismair.cchain.contracts.Contract
import com.ismair.cchain.contracts.cash.data.CCash
import com.ismair.cchain.contracts.trade.model.TradeConfirmation
import com.ismair.cchain.contracts.trade.model.TradeRejection
import com.ismair.cchain.contracts.trade.model.TradeRequest
import com.ismair.cchain.data.daxMap
import com.ismair.cchain.extensions.forEachNonEqualPair
import com.ismair.cchain.services.DepotService
import de.transbase.cchain.wrapper.TDBWrapper
import java.text.SimpleDateFormat
import java.util.*

class TradeContract(tdbWrapper: TDBWrapper) : Contract(tdbWrapper) {
    private val responses = tdbWrapper.getParsedSentTransactions(listOf(TradeConfirmation::class, TradeRejection::class))
    private val processedRequestIds = responses.map { it.document.requestId }.toMutableSet()
    private val depotService = DepotService(responses.mapNotNull { it.document as? TradeConfirmation })
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    override fun run() {
        println("loading open requests ...")

        val openRequests = tdbWrapper
                .getParsedReceivedTransactions<TradeRequest>()
                .filter { !processedRequestIds.contains(it.id) }

        println("verifying ${openRequests.size} open requests ...")

        val validRequests = openRequests.mapNotNull {
            val request = it.document
            val dateLimitParsed = try { dateFormat.parse(request.dateLimit) } catch (e: Exception) { null }

            val message = when {
                it.sender != CCash.publicKeyPKCS8 -> "sender is not accepted"
                request.user.isEmpty() -> "user is required"
                !daxMap.containsKey(request.isin) -> "isin is not valid"
                request.shareCount <= 0 -> "share count has to be greater than zero"
                request.priceLimit <= 0 -> "price limit has to be greater than zero"
                dateLimitParsed == null -> "date limit could not be parsed"
                dateLimitParsed.before(Date()) -> "request is expired"
                request.mode == TradeRequest.Mode.SELL &&
                        !depotService.hasEnoughShares(it.sender, request.isin, request.shareCount) -> "selling shares without property"
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
                tdbWrapper.createNewTransaction(transaction1.chain, transaction1.sender, confirmation1, true)
                depotService.add(transaction1.sender, confirmation1)
                matchedRequestIds.add(id1)

                val confirmation2 = TradeConfirmation(id2, request2, price)
                tdbWrapper.createNewTransaction(transaction2.chain, transaction2.sender, confirmation2, true)
                depotService.add(transaction2.sender, confirmation2)
                matchedRequestIds.add(id2)
            }
        }

        processedRequestIds.addAll(matchedRequestIds)
    }
}