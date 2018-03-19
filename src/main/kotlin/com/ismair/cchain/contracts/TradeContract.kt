package com.ismair.cchain.contracts

import com.ismair.cchain.data.daxMap
import com.ismair.cchain.model.cheat.CheatConfirmation
import com.ismair.cchain.model.cheat.CheatMode
import com.ismair.cchain.model.cheat.CheatRequest
import com.ismair.cchain.model.trade.TradeConfirmation
import com.ismair.cchain.model.trade.TradeMode
import com.ismair.cchain.model.trade.TradeRejection
import com.ismair.cchain.model.trade.TradeRequest
import com.ismair.cchain.services.DepotService
import de.transbase.cchain.wrapper.TDBWrapper
import java.text.SimpleDateFormat
import java.util.*

class TradeContract(
        tdbWrapper: TDBWrapper,
        private val tradePublicKeyPKCS8: String,
        private val cashPublicKeyPKCS8: String
) : Contract(tdbWrapper) {
    private val responses = tdbWrapper.getParsedSentTransactions(listOf(
            CheatConfirmation::class, TradeConfirmation::class, TradeRejection::class))
    private val processedRequestIds = responses.map { it.document.requestId }.toMutableSet()
    private val depotService = DepotService(responses.mapNotNull { it.document as? TradeConfirmation })
    private val openTradeTransactions = mutableListOf<TDBWrapper.ParsedTransaction<TradeRequest>>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    override fun run() {
        println("checking open trade requests for expiration ...")

        val expiredTradeTransactions = mutableListOf<TDBWrapper.ParsedTransaction<TradeRequest>>()
        openTradeTransactions.forEach {
            val (chain, id, sender, _, request) = it
            val dateLimitParsed = try { dateFormat.parse(request.dateLimit) } catch (e: Exception) { null }

            val message = when {
                dateLimitParsed == null -> "date limit could not be parsed"
                dateLimitParsed.before(Date()) -> "request is expired"
                else -> null
            }

            if (message != null) {
                println("rejecting trade request with id = $id with reason '$message' ...")

                val rejection = TradeRejection(id, request, message)
                tdbWrapper.createNewTransaction(chain, sender, rejection, true)
                expiredTradeTransactions.add(it)
            }
        }
        openTradeTransactions.removeAll(expiredTradeTransactions)

        println("loading open requests ...")

        val openRequests = tdbWrapper
                .getParsedReceivedTransactions(listOf(CheatRequest::class, TradeRequest::class))
                .filter { !processedRequestIds.contains(it.id) }

        println("processing ${openRequests.size} open requests ...")

        openRequests.forEach {
            val (chain, id, sender, _, request) = it

            when (request) {
                is CheatRequest -> handleCheatRequest(chain, id, sender, request)
                is TradeRequest -> handleTradeRequest(chain, id, sender, request)
            }

            processedRequestIds.add(it.id)
        }

        println("done!")
    }

    private fun handleCheatRequest(chain: String, id: Int, sender: String, request: CheatRequest) {
        val user = sender
        val (mode) = request

        println("processing cheat request with mode '$mode' ...")

        when (mode) {
            CheatMode.MONEY -> {}
            CheatMode.EMPLOYEE -> {}
            CheatMode.SHARES -> {
                val isin = daxMap.keys.first()
                val shareCount = 100
                val priceLimit = 42
                val price = 42
                val confirmation = TradeConfirmation(id, TradeMode.BUY, user, isin, shareCount, priceLimit, price)
                tdbWrapper.createNewTransaction(chain, cashPublicKeyPKCS8, confirmation, true)
                depotService.add(confirmation)
            }
        }

        val confirmation = CheatConfirmation(id, mode)
        tdbWrapper.createNewTransaction(chain, user, confirmation, true)
    }

    private fun handleTradeRequest(chain: String, id: Int, sender: String, request: TradeRequest) {
        val (mode, user, isin, shareCount, priceLimit, dateLimit) = request
        val dateLimitParsed = try { dateFormat.parse(dateLimit) } catch (e: Exception) { null }

        println("verifying trade request ...")

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
            println("rejecting trade request with id = $id with reason '$message' ...")

            val rejection = TradeRejection(id, request, message)
            tdbWrapper.createNewTransaction(chain, sender, rejection, true)
        } else {
            println("processing trade request of $shareCount shares of '$isin' with a price limit of $priceLimit cEuro ...")

            val match = openTradeTransactions.find {
                val (mode2, _, isin2, shareCount2, priceLimit2, _) = it.document
                mode == TradeMode.BUY && mode2 == TradeMode.SELL &&
                        isin == isin2 && shareCount == shareCount2 && priceLimit >= priceLimit2
            }

            if (match != null) {
                val (chain2, id2, sender2, _, request2) = match
                val (mode2, user2, isin2, shareCount2, priceLimit2, _) = request2
                val price = (priceLimit + priceLimit2) / 2

                println("confirming trades for $shareCount shares of $isin at price $price ...")

                val confirmation = TradeConfirmation(id, mode, user, isin, shareCount, priceLimit, price)
                tdbWrapper.createNewTransaction(chain, sender, confirmation, true)
                depotService.add(confirmation)

                val confirmation2 = TradeConfirmation(id2, mode2, user2, isin2, shareCount2, priceLimit2, price)
                tdbWrapper.createNewTransaction(chain2, sender2, confirmation2, true)
                depotService.add(confirmation2)
                openTradeTransactions.remove(match)
            } else {
                openTradeTransactions.add(TDBWrapper.ParsedTransaction(chain, id, sender, tradePublicKeyPKCS8, request))
            }
        }
    }
}