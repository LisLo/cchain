package com.ismair.cchain.cash

import com.ismair.cchain.Contract
import com.ismair.cchain.cash.model.Confirmation
import com.ismair.cchain.cash.model.Transfer
import de.transbase.cchain.wrapper.TDBWrapper

class CashContract(tdbWrapper: TDBWrapper) : Contract(tdbWrapper) {
    override fun run() {
        println("loading confirmations ...")

        val processedTransferIds = tdbWrapper.getParsedSentTransactions<Confirmation>().map { it.document.transferId }.toMutableSet()

        println("${processedTransferIds.size} confirmations found")

        while (true) {
            try {
                println("loading open transfers ...")

                val openTransfers = tdbWrapper.getParsedReceivedTransactions<Transfer>().filter { !processedTransferIds.contains(it.id) }

                println("processing ${openTransfers.size} open transfers ...")

                openTransfers.forEach {
                    val transfer = it.document

                    println("processing transfer of ${transfer.amount} cEuro with purpose ${transfer.purpose}")

                    val confirmation1 = Confirmation(it.id, transfer.receiver, -transfer.amount, transfer.purpose)
                    tdbWrapper.createNewTransaction(it.chain, it.sender, confirmation1, true)

                    val confirmation2 = Confirmation(it.id, it.sender, transfer.amount, transfer.purpose)
                    tdbWrapper.createNewTransaction(it.chain, transfer.receiver, confirmation2, true)

                    processedTransferIds.add(it.id)
                }
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting contract ...")
            }
        }
    }
}