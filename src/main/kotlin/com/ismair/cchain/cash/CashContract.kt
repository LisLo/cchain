package com.ismair.cchain.cash

import com.ismair.cchain.cash.model.DepotRequest
import com.ismair.cchain.cash.model.TransferConfirmation
import com.ismair.cchain.cash.model.TransferRejection
import com.ismair.cchain.cash.model.TransferRequest
import com.ismair.cchain.contract.Contract
import de.transbase.cchain.wrapper.TDBWrapper

class CashContract : Contract() {
    override fun run(tdbWrapper: TDBWrapper) {
        println("loading responses ...")

        val responses = tdbWrapper.getParsedSentTransactions(listOf(TransferConfirmation::class, TransferRejection::class))
        val processedRequestIds = responses.map { it.document.requestId }.toMutableSet()

        println("${processedRequestIds.size} responses found")

        while (true) {
            try {
                println("loading open requests ...")

                val openRequests = tdbWrapper
                        .getParsedReceivedTransactions(listOf(TransferRequest::class, DepotRequest::class))
                        .filter { !processedRequestIds.contains(it.id) }

                println("processing ${openRequests.size} open requests ...")

                openRequests.forEach {
                    val request = it.document

                    if (request is TransferRequest) {
                        println("processing transfer request of ${request.amount} cEuro with purpose '${request.purpose}'")

                        val confirmation1 = TransferConfirmation(it.id, request, TransferConfirmation.Mode.DEBIT)
                        tdbWrapper.createNewTransaction(it.chain, it.sender, confirmation1, true)

                        val confirmation2 = TransferConfirmation(it.id, request, TransferConfirmation.Mode.CREDIT)
                        tdbWrapper.createNewTransaction(it.chain, request.user, confirmation2, true)

                        processedRequestIds.add(it.id)
                    } else if (request is DepotRequest) {
                        println("processing depot request of ${request.shareCount} shares of '${request.isin}' with price limit ${request.priceLimit} cEuro")
                    }
                }
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting contract ...")
            }
        }
    }
}