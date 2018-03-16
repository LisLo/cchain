package com.ismair.cchain.contracts.cash

import com.ismair.cchain.contracts.Contract
import com.ismair.cchain.contracts.cash.model.*
import com.ismair.cchain.contracts.trade.model.TradeConfirmation
import com.ismair.cchain.contracts.trade.model.TradeRejection
import com.ismair.cchain.contracts.trade.model.TradeRequest
import com.ismair.cchain.data.daxMap
import com.ismair.cchain.services.BalanceService
import com.ismair.cchain.services.DepotService
import com.ismair.cchain.ui.depot.data.CTrade
import de.transbase.cchain.wrapper.TDBWrapper
import java.text.SimpleDateFormat
import java.util.*

class CashContract(tdbWrapper: TDBWrapper) : Contract(tdbWrapper) {
    private val responses = tdbWrapper.getParsedSentTransactions(listOf(
            TransferConfirmation::class, TransferRejection::class,
            DepotConfirmation::class, DepotRejection::class,
            TradeConfirmation::class, TradeRejection::class))
    private val processedRequestIds = responses.map { it.document.requestId }.toMutableSet()
    private val balanceService = BalanceService(responses.mapNotNull { it.document as? TransferConfirmation })
    private val depotService = DepotService(responses.mapNotNull { it.document as? TradeConfirmation })
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    override fun run() {
        println("loading open documents ...")

        val openDocuments = tdbWrapper
                .getParsedReceivedTransactions(listOf(
                        TransferRequest::class, DepotRequest::class,
                        TradeConfirmation::class, TradeRejection::class))
                .filter { !processedRequestIds.contains(it.id) }

        println("processing ${openDocuments.size} open documents ...")

        openDocuments.forEach {
            val document = it.document
            val id = it.id
            val sender = it.sender
            val chain = it.chain

            when (document) {
                is TransferRequest -> handleTransferRequest(chain, id, sender, document)
                is DepotRequest -> handleDepotRequest(chain, id, sender, document)
                is TradeConfirmation -> handleTradeConfirmation(chain, id, sender, document)
                is TradeRejection -> handleTradeRejection(chain, id, sender, document)
            }

            processedRequestIds.add(it.id)
        }
    }

    private fun handleTransferRequest(chain: String, id: Int, sender: String, request: TransferRequest) {
        val payer = sender
        val payee = request.payee
        val amount = request.amount
        val purpose = request.purpose

        println("verifying transfer request ...")

        val message = when {
            payee.isEmpty() -> "payee is required"
            amount <= 0 -> "amount has to be greater than zero"
            purpose.isEmpty() -> "purpose is required"
            !balanceService.hasEnoughMoney(payer, amount) -> "amount is greater than the balance"
            else -> null
        }

        if (message != null) {
            println("rejecting transfer request with id = $id with reason '$message' ...")

            val rejection = TransferRejection(id, request, message)
            tdbWrapper.createNewTransaction(chain, payer, rejection, true)
        } else {
            println("processing transfer request of $amount cEuro with purpose '$purpose'")

            val confirmation = TransferConfirmation(id, payer, payee, amount, purpose)
            tdbWrapper.createNewTransaction(chain, payer, confirmation, true)
            tdbWrapper.createNewTransaction(chain, payee, confirmation, true)
            balanceService.add(confirmation)
        }
    }

    private fun handleDepotRequest(chain: String, id: Int, sender: String, request: DepotRequest) {
        val user = sender
        val mode = request.mode
        val isin = request.isin
        val shareCount = request.shareCount
        val priceLimit = request.priceLimit
        val dateLimit = try { dateFormat.parse(request.dateLimit) } catch (e: Exception) { null }

        println("verifying depot request ...")

        val message = when {
            user.isEmpty() -> "user is required"
            !daxMap.containsKey(isin) -> "isin is not valid"
            shareCount <= 0 -> "share count has to be greater than zero"
            priceLimit <= 0 -> "price limit has to be greater than zero"
            dateLimit == null -> "date limit could not be parsed"
            dateLimit.before(Date()) -> "request is expired"
            mode == TradeRequest.Mode.SELL &&
                    !depotService.hasEnoughShares(user, isin, shareCount) -> "selling shares without property"
            else -> null
        }

        if (message != null) {
            println("rejecting depot request with id = $id with reason '$message' ...")

            val rejection = DepotRejection(id, request, message)
            tdbWrapper.createNewTransaction(chain, user, rejection, true)
        } else {
            println("processing depot request of $shareCount shares of '$isin' with a price limit of $priceLimit cEuro")

            val tradeRequest = TradeRequest(request)
            tdbWrapper.createNewTransaction(chain, CTrade.publicKeyPKCS8, tradeRequest, true)

            val confirmation = DepotConfirmation(id, request)
            tdbWrapper.createNewTransaction(chain, user, confirmation, true)
        }
    }

    private fun handleTradeConfirmation(chain: String, id: Int, sender: String, confirmation: TradeConfirmation) {
        val request = confirmation.request
        val user = request.user
        val isin = request.isin
        val shareCount = request.shareCount
        val price = confirmation.price

        println("forward trade confirmation of $shareCount shares of '$isin' with price $price cEuro")

        tdbWrapper.createNewTransaction(chain, user, confirmation, true)
        depotService.add(user, confirmation)
    }

    private fun handleTradeRejection(chain: String, id: Int, sender: String, rejection: TradeRejection) {
        val request = rejection.request
        val user = request.user
        val isin = request.isin
        val shareCount = request.shareCount
        val priceLimit = request.priceLimit

        println("forward trade rejection of $shareCount shares of '$isin' with a price limit of $priceLimit cEuro")

        tdbWrapper.createNewTransaction(chain, user, rejection, true)
    }
}