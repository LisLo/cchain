package com.ismair.cchain.cash

import com.ismair.cchain.cash.model.*
import com.ismair.cchain.contract.Contract
import com.ismair.cchain.trade.data.daxMap
import com.ismair.cchain.trade.model.TradeConfirmation
import com.ismair.cchain.trade.model.TradeRejection
import com.ismair.cchain.trade.model.TradeRequest
import com.ismair.cchain.ui.depot.data.CTrade
import de.transbase.cchain.wrapper.TDBWrapper
import java.text.SimpleDateFormat
import java.util.*

class CashContract : Contract() {
    override fun run(tdbWrapper: TDBWrapper) {
        println("loading responses ...")

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val responses = tdbWrapper.getParsedSentTransactions(listOf(TransferConfirmation::class, TransferRejection::class))
        val processedRequestIds = responses.map { it.document.requestId }.toMutableSet()
        val properties = responses
                .mapNotNull { it.document as? TransferConfirmation }
                .groupBy { it.request.user }
                .toList()
                .associate {
                    it.first to it.second.sumBy {
                        val amount = it.request.amount
                        if (it.mode == TransferConfirmation.Mode.CREDIT) amount else -amount
                    }
                }

        println("${processedRequestIds.size} responses found")

        while (true) {
            try {
                println("loading open requests ...")

                val openRequests = tdbWrapper
                        .getParsedReceivedTransactions(listOf(TransferRequest::class, DepotRequest::class, TradeConfirmation::class, TradeRejection::class))
                        .filter { !processedRequestIds.contains(it.id) }

                println("processing ${openRequests.size} open requests ...")

                openRequests.forEach {
                    val request = it.document

                    if (request is TransferRequest) {
                        println("verifying transfer request ...")

                        val property = properties[request.user]

                        val message = when {
                            request.user.isEmpty() -> "user is required"
                            request.amount <= 0 -> "amount has to be greater than zero"
                            request.purpose.isEmpty() -> "purpose is required"
                            property == null || property < request.amount -> "amount is greater than the balance"
                            else -> null
                        }

                        if (message != null) {
                            println("rejecting transfer request with id = ${it.id} with reason '$message' ...")

                            val rejection = TransferRejection(it.id, request, message)
                            tdbWrapper.createNewTransaction(it.chain, it.sender, rejection, true)
                        } else {
                            println("processing transfer request of ${request.amount} cEuro with purpose '${request.purpose}'")

                            val confirmation1 = TransferConfirmation(it.id, request, TransferConfirmation.Mode.DEBIT)
                            tdbWrapper.createNewTransaction(it.chain, it.sender, confirmation1, true)

                            val confirmation2 = TransferConfirmation(it.id, request, TransferConfirmation.Mode.CREDIT)
                            tdbWrapper.createNewTransaction(it.chain, request.user, confirmation2, true)
                        }
                    } else if (request is DepotRequest) {
                        println("verifying depot request ...")

                        val dateLimitParsed = try { sdf.parse(request.dateLimit) } catch (e: Exception) { null }

                        val message = when {
                            request.name.isEmpty() -> "name is required"
                            !daxMap.containsKey(request.isin) -> "isin is not valid"
                            request.shareCount <= 0 -> "share count has to be greater than zero"
                            request.priceLimit <= 0 -> "price limit has to be greater than zero"
                            dateLimitParsed == null -> "date limit could not be parsed"
                            dateLimitParsed.before(Date()) -> "request is expired"
                            else -> null
                        }

                        if (message != null) {
                            println("rejecting depot request with id = ${it.id} with reason '$message' ...")

                            val rejection = DepotRejection(it.id, request, message)
                            tdbWrapper.createNewTransaction(it.chain, it.sender, rejection, true)
                        } else {
                            println("processing depot request of ${request.shareCount} shares of '${request.isin}' with price limit ${request.priceLimit} cEuro")

                            val tradeRequest = TradeRequest(request)
                            tdbWrapper.createNewTransaction("C-trade", CTrade.publicKeyPKCS8, tradeRequest, true)

                            if (tradeRequest.mode == TradeRequest.Mode.BUY) {
                                val amount = request.priceLimit * request.shareCount
                                val purpose = "debit money for ${request.shareCount} shares of '${request.isin}' with price limit ${request.priceLimit} cEuro"
                                val transferRequest = TransferRequest(it.sender, amount, purpose)
                                val transferConfirmation = TransferConfirmation(it.id, transferRequest, TransferConfirmation.Mode.DEBIT)
                                tdbWrapper.createNewTransaction(it.chain, it.sender, transferConfirmation, true)
                            }

                            val execution = DepotExecution(it.id, request)
                            tdbWrapper.createNewTransaction(it.chain, it.sender, execution, true)
                        }
                    } else if (request is TradeConfirmation) {
                        println("processing trade confirmation of ${request.request.shareCount} shares of '${request.request.isin}' with price ${request.price} cEuro")

                        if (request.request.mode == TradeRequest.Mode.SELL) {
                            val amount = request.price * request.request.shareCount
                            val purpose = "credit money for ${request.request.shareCount} shares of '${request.request.isin}' with price ${request.price} cEuro"
                            val transferRequest = TransferRequest(it.sender, amount, purpose)
                            val transferConfirmation = TransferConfirmation(it.id, transferRequest, TransferConfirmation.Mode.DEBIT)
                            tdbWrapper.createNewTransaction(it.chain, it.sender, transferConfirmation, true)
                        }

                        //val confirmation = DepotConfirmation(request)
                        //tdbWrapper.createNewTransaction()
                    } else if (request is TradeRejection) {
                        println("processing trade rejection of ${request.request.shareCount} shares of '${request.request.isin}' with price limit ${request.request.priceLimit} cEuro")

                        // return money if it was buy

                        //val rejection = DepotRejection()
                        //tdbWrapper.createNewTransaction()
                    }

                    processedRequestIds.add(it.id)
                }
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting contract ...")
            }
        }
    }
}