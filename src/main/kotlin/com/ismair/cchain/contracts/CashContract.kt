package com.ismair.cchain.contracts

import com.ismair.cchain.abstracts.Contract
import com.ismair.cchain.model.trade.TradeConfirmation
import com.ismair.cchain.model.trade.TradeMode
import com.ismair.cchain.model.trade.TradeRejection
import com.ismair.cchain.model.trade.TradeRequest
import com.ismair.cchain.data.daxMap
import com.ismair.cchain.model.depot.DepotConfirmation
import com.ismair.cchain.model.depot.DepotRejection
import com.ismair.cchain.model.depot.DepotRequest
import com.ismair.cchain.model.transfer.TransferConfirmation
import com.ismair.cchain.model.transfer.TransferRejection
import com.ismair.cchain.model.transfer.TransferRequest
import com.ismair.cchain.services.BalanceService
import com.ismair.cchain.services.DepotService
import de.transbase.cchain.wrapper.TDBWrapper
import java.text.SimpleDateFormat
import java.util.*

class CashContract(tdbWrapper: TDBWrapper, private val tradePublicKeyPKCS8: String) : Contract(tdbWrapper) {
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
            val (chain, id, sender, _, document) = it

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
        val (payee, amount, purpose) = request

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
        val (mode, isin, shareCount, priceLimit, dateLimit) = request
        val dateLimitParsed = try { dateFormat.parse(dateLimit) } catch (e: Exception) { null }

        println("verifying depot request ...")

        val message = when {
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
            println("rejecting depot request with id = $id with reason '$message' ...")

            val rejection = DepotRejection(id, request, message)
            tdbWrapper.createNewTransaction(chain, user, rejection, true)
        } else {
            println("processing depot request of $shareCount shares of '$isin' with a price limit of $priceLimit cEuro")

            val tradeRequest = TradeRequest(user, request)
            tdbWrapper.createNewTransaction(chain, tradePublicKeyPKCS8, tradeRequest, true)

            val confirmation = DepotConfirmation(id, request)
            tdbWrapper.createNewTransaction(chain, user, confirmation, true)
        }
    }

    private fun handleTradeConfirmation(chain: String, id: Int, sender: String, confirmation: TradeConfirmation) {
        val (_, _, user, isin, shareCount, price) = confirmation

        println("forward trade confirmation of $shareCount shares of '$isin' with a price of $price cEuro")

        tdbWrapper.createNewTransaction(chain, user, confirmation, true)
        depotService.add(confirmation)
    }

    private fun handleTradeRejection(chain: String, id: Int, sender: String, rejection: TradeRejection) {
        val request = rejection.request
        val (_, user, isin, shareCount, priceLimit) = request

        println("forward trade rejection of $shareCount shares of '$isin' with a price limit of $priceLimit cEuro")

        tdbWrapper.createNewTransaction(chain, user, rejection, true)
    }
}