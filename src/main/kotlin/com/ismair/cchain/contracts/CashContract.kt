package com.ismair.cchain.contracts

import com.ismair.cchain.data.daxMap
import com.ismair.cchain.model.cheat.CheatConfirmation
import com.ismair.cchain.model.cheat.CheatMode
import com.ismair.cchain.model.cheat.CheatRequest
import com.ismair.cchain.model.depot.DepotConfirmation
import com.ismair.cchain.model.depot.DepotMode
import com.ismair.cchain.model.depot.DepotRejection
import com.ismair.cchain.model.depot.DepotRequest
import com.ismair.cchain.model.right.RightConfirmation
import com.ismair.cchain.model.right.RightRejection
import com.ismair.cchain.model.right.RightRequest
import com.ismair.cchain.model.trade.TradeConfirmation
import com.ismair.cchain.model.trade.TradeMode
import com.ismair.cchain.model.trade.TradeRejection
import com.ismair.cchain.model.trade.TradeRequest
import com.ismair.cchain.model.transfer.TransferConfirmation
import com.ismair.cchain.model.transfer.TransferRejection
import com.ismair.cchain.model.transfer.TransferRequest
import com.ismair.cchain.services.AuthorizationService
import com.ismair.cchain.services.BalanceService
import com.ismair.cchain.services.DepotService
import de.transbase.cchain.wrapper.TDBWrapper
import java.text.SimpleDateFormat
import java.util.*

class CashContract(
        tdbWrapper: TDBWrapper,
        private val cashPublicKeyPKCS8: String,
        private val tradePublicKeyPKCS8: String
) : Contract(tdbWrapper) {
    private val responses = tdbWrapper.getParsedSentTransactions(listOf(
            CheatConfirmation::class,
            TransferConfirmation::class, TransferRejection::class,
            RightConfirmation::class, RightRejection::class,
            DepotConfirmation::class, DepotRejection::class,
            TradeConfirmation::class, TradeRejection::class))
    private val processedRequestIds = responses.map { it.document.requestId }.toMutableSet()
    private val balanceService = BalanceService(responses.mapNotNull { it.document as? TransferConfirmation })
    private val authorizationService = AuthorizationService(responses.mapNotNull { it.document as? RightConfirmation })
    private val depotService = DepotService(responses.mapNotNull { it.document as? TradeConfirmation })
    private val acceptedEmployees = mutableSetOf<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    override fun run() {
        println("loading open documents ...")

        val openDocuments = tdbWrapper
                .getParsedReceivedTransactions(listOf(
                        CheatRequest::class, TransferRequest::class, RightRequest::class, DepotRequest::class,
                        TradeConfirmation::class, TradeRejection::class))
                .filter { !processedRequestIds.contains(it.id) }

        println("processing ${openDocuments.size} open documents ...")

        openDocuments.forEach {
            val (chain, id, sender, _, document) = it

            when (document) {
                is CheatRequest -> handleCheatRequest(chain, id, sender, document)
                is TransferRequest -> handleTransferRequest(chain, id, sender, document)
                is RightRequest -> handleRightRequest(chain, id, sender, document)
                is DepotRequest -> handleDepotRequest(chain, id, sender, document)
                is TradeConfirmation -> handleTradeConfirmation(chain, id, sender, document)
                is TradeRejection -> handleTradeRejection(chain, id, sender, document)
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
            CheatMode.MONEY -> {
                val amount = 1000
                val purpose = "cheat"
                val confirmation = TransferConfirmation(id, cashPublicKeyPKCS8, user, amount, purpose)
                tdbWrapper.createNewTransaction(chain, user, confirmation, true)
                balanceService.add(confirmation)
            }
            CheatMode.EMPLOYEE -> acceptedEmployees.add(user)
            CheatMode.SHARES -> {}
        }

        val confirmation = CheatConfirmation(id, mode)
        tdbWrapper.createNewTransaction(chain, user, confirmation, true)
    }

    private fun handleTransferRequest(chain: String, id: Int, sender: String, request: TransferRequest) {
        val payer = sender
        val (payee, amount, purpose) = request

        println("verifying transfer request ...")

        val message = when {
            payee.isEmpty() -> "payee is required"
            amount <= 0 -> "amount has to be greater than zero"
            purpose.isEmpty() -> "purpose is required"
            //TODO !balanceService.hasEnoughMoney(payer, amount) -> "amount is greater than the balance"
            else -> null
        }

        if (message != null) {
            println("rejecting transfer request with id = $id with reason '$message' ...")

            val rejection = TransferRejection(id, request, message)
            tdbWrapper.createNewTransaction(chain, payer, rejection, true)
        } else {
            println("processing transfer request of $amount cEuro with purpose '$purpose' ...")

            val confirmation = TransferConfirmation(id, payer, payee, amount, purpose)
            tdbWrapper.createNewTransaction(chain, payer, confirmation, true)
            tdbWrapper.createNewTransaction(chain, payee, confirmation, true)
            balanceService.add(confirmation)
        }
    }

    private fun handleRightRequest(chain: String, id: Int, sender: String, request: RightRequest) {
        val (user) = request

        println("verifying right request ...")

        val message = when {
            user.isEmpty() -> "user is required"
            else -> null
        }

        if (message != null) {
            println("rejecting right request with id = $id with reason '$message' ...")

            val rejection = RightRejection(id, request, message)
            tdbWrapper.createNewTransaction(chain, sender, rejection, true)
        } else {
            println("processing right request for user '$user' ...")

            val confirmation = RightConfirmation(id, user)
            tdbWrapper.createNewTransaction(chain, sender, confirmation, true)
        }
    }

    private fun handleDepotRequest(chain: String, id: Int, sender: String, request: DepotRequest) {
        val user = sender
        val (mode, isin, shareCount, priceLimit, dateLimit) = request
        val dateLimitParsed = try { dateFormat.parse(dateLimit) } catch (e: Exception) { null }

        println("verifying depot request ...")

        val message = when {
            !daxMap.containsKey(isin) -> "isin is not valid"
            shareCount <= 0 -> "share count has to be greater than zero"
            priceLimit <= 0 -> "price limit has to be greater than zero"
            dateLimitParsed == null -> "date limit could not be parsed"
            dateLimitParsed.before(Date()) -> "request is expired"
            authorizationService.hasRight(user) -> "user is not authorized for depot requests"
            /*TODO mode == TradeMode.SELL &&
                    !depotService.hasEnoughShares(user, isin, shareCount) -> "selling shares without property"*/
            else -> null
        }

        if (message != null) {
            println("rejecting depot request with id = $id with reason '$message' ...")

            val rejection = DepotRejection(id, request, message)
            tdbWrapper.createNewTransaction(chain, user, rejection, true)
        } else {
            println("processing depot request of $shareCount shares of '$isin' with a price limit of $priceLimit cEuro ...")

            val mappedMode = if (request.mode == DepotMode.BUY) TradeMode.BUY else TradeMode.SELL
            val tradeRequest = TradeRequest(mappedMode, user, isin, shareCount, priceLimit, dateLimit)
            tdbWrapper.createNewTransaction(chain, tradePublicKeyPKCS8, tradeRequest, true)

            if (tradeRequest.mode == TradeMode.BUY) {
                val amount = priceLimit * shareCount
                val purpose = "freeze for the request of $shareCount shares of '$isin' with a price limit of $priceLimit cEuro"
                val confirmation = TransferConfirmation(id, user, cashPublicKeyPKCS8, amount, purpose)
                tdbWrapper.createNewTransaction(chain, sender, confirmation, true)
            }

            val confirmation = DepotConfirmation(id, mode, isin, shareCount, priceLimit, dateLimit)
            tdbWrapper.createNewTransaction(chain, user, confirmation, true)
        }
    }

    private fun handleTradeConfirmation(chain: String, id: Int, sender: String, confirmation: TradeConfirmation) {
        val (_, mode, user, isin, shareCount, priceLimit, price) = confirmation

        println("verifying trade confirmation ...")

        /*TODO if (sender != tradePublicKeyPKCS8) {
            return
        }*/

        println("forwarding trade confirmation of $shareCount shares of '$isin' with a price of $price cEuro ...")

        tdbWrapper.createNewTransaction(chain, user, confirmation, true)
        depotService.add(confirmation)

        if (mode == TradeMode.SELL) {
            val amount = price * shareCount
            val purpose = "sell of $shareCount shares of '$isin' with a price of $price cEuro"
            val confirmation2 = TransferConfirmation(id, cashPublicKeyPKCS8, user, amount, purpose)
            tdbWrapper.createNewTransaction(chain, user, confirmation2, true)
        } else if (price != priceLimit) {
            val amount = (priceLimit - price) * shareCount
            val purpose = "purchase of $shareCount shares of '$isin' with a price difference of $amount cEuro"
            val confirmation2 = TransferConfirmation(id, cashPublicKeyPKCS8, user, amount, purpose)
            tdbWrapper.createNewTransaction(chain, sender, confirmation2, true)
        }
    }

    private fun handleTradeRejection(chain: String, id: Int, sender: String, rejection: TradeRejection) {
        val request = rejection.request
        val (mode, user, isin, shareCount, priceLimit) = request

        println("verifying trade rejection ...")

        /*TODO if (sender != tradePublicKeyPKCS8) {
            return
        }*/

        println("forwarding trade rejection of $shareCount shares of '$isin' with a price limit of $priceLimit cEuro ...")

        tdbWrapper.createNewTransaction(chain, user, rejection, true)

        if (mode == TradeMode.BUY) {
            val amount = priceLimit * shareCount
            val purpose = "payback for the request of $shareCount shares of '$isin' with a price limit of $priceLimit cEuro"
            val confirmation = TransferConfirmation(id, cashPublicKeyPKCS8, user, amount, purpose)
            tdbWrapper.createNewTransaction(chain, sender, confirmation, true)
        }
    }
}