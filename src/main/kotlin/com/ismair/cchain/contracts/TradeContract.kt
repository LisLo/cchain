package com.ismair.cchain.contracts

import com.ismair.cchain.abstracts.Contract
import com.ismair.cchain.data.daxMap
import com.ismair.cchain.extensions.forEachNonEqualPair
import com.ismair.cchain.model.trade.TradeConfirmation
import com.ismair.cchain.model.trade.TradeMode
import com.ismair.cchain.model.trade.TradeRejection
import com.ismair.cchain.model.trade.TradeRequest
import com.ismair.cchain.services.DepotService
import de.transbase.cchain.wrapper.TDBWrapper
import java.text.SimpleDateFormat
import java.util.*

class TradeContract(tdbWrapper: TDBWrapper, private val cashPublicKeyPKCS8: String) : Contract(tdbWrapper) {
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
            val (chain, id, sender, _, request) = it
            val (mode, user, isin, shareCount, priceLimit, dateLimit) = request
            val dateLimitParsed = try { dateFormat.parse(dateLimit) } catch (e: Exception) { null }

            val message = when {
                sender != cashPublicKeyPKCS8 -> "sender is not accepted"
                user.isEmpty() -> "user is required"
                !daxMap.containsKey(isin) -> "isin is not valid"
                shareCount <= 0 -> "share count has to be greater than zero"
                priceLimit <= 0 -> "price limit has to be greater than zero"
                dateLimitParsed == null -> "date limit could not be parsed"
                dateLimitParsed.before(Date()) -> "request is expired"
                mode == TradeMode.SELL &&
                        !depotService.hasEnoughShares(user, isin, shareCount) -> "selling shares without property"
                else -> null
            }

            if (message != null) {
                println("rejecting request with id = $id with reason '$message' ...")

                val rejection = TradeRejection(id, request, message)
                tdbWrapper.createNewTransaction(chain, sender, rejection, true)
                processedRequestIds.add(id)
                null
            } else {
                it
            }
        }

        println("search matching requests ...")

        val matchedRequestIds = mutableSetOf<Int>()
        validRequests.forEachNonEqualPair { transaction1, transaction2 ->
            val (chain1, id1, sender1, _, request1) = transaction1
            val (chain2, id2, sender2, _, request2) = transaction2
            val (mode1, user1, isin1, shareCount1, priceLimit1, _) = request1
            val (mode2, user2, isin2, shareCount2, priceLimit2, _) = request2

            if (!matchedRequestIds.contains(id1) && !matchedRequestIds.contains(id2) &&
                    mode1 == TradeMode.BUY && mode2 == TradeMode.SELL &&
                    isin1 == isin2 && shareCount1 == shareCount2 && priceLimit1 >= priceLimit2) {
                val price = (priceLimit1 + priceLimit2) / 2

                println("confirming trades for $shareCount1 shares of $isin1 at price $price ...")

                val confirmation1 = TradeConfirmation(id1, mode1, user1, isin1, shareCount1, priceLimit1, price)
                tdbWrapper.createNewTransaction(chain1, sender1, confirmation1, true)
                depotService.add(confirmation1)
                matchedRequestIds.add(id1)

                val confirmation2 = TradeConfirmation(id2, mode2, user2, isin2, shareCount2, priceLimit2, price)
                tdbWrapper.createNewTransaction(chain2, sender2, confirmation2, true)
                depotService.add(confirmation2)
                matchedRequestIds.add(id2)
            }
        }

        processedRequestIds.addAll(matchedRequestIds)
    }
}