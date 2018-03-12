package com.ismair.cchain.cash

import com.ismair.cchain.Contract
import com.ismair.cchain.cash.model.Booking
import com.ismair.cchain.cash.model.Confirmation
import com.ismair.cchain.cash.model.Transfer
import com.ismair.cchain.services.TDBService
import kotlinx.serialization.json.JSON

class CashContract(tdbService: TDBService) : Contract(tdbService) {
    override fun run() {
        println("starting C-cash ...")

        println("loading all sent transactions ...")

        val processedTransferIds = mutableSetOf<Int>()
        tdbService.getSentTransactions().forEach {
            if (it.document != null) {
                try {
                    val confirmation = JSON.parse<Confirmation>(it.document)
                    processedTransferIds.add(confirmation.transferId)
                } catch (e: Exception) {
                    println("could not parse transaction with id = ${it.id}")
                }
            }
        }

        println("${processedTransferIds.size} transfers already processed")

        while (true) {
            try {
                println("loading all received transactions and calculating open transfers ...")

                val openBookings = mutableListOf<Booking>()
                tdbService.getReceivedTransactions().forEach {
                    if (!processedTransferIds.contains(it.id) && it.document != null) {
                        try {
                            val transfer = JSON.parse<Transfer>(it.document)
                            openBookings.add(Booking(it.id, it.chain, it.sender, transfer.receiver, transfer.amount, transfer.purpose))
                        } catch (e: Exception) {
                            println("could not parse transaction with id = ${it.id}")
                        }
                    }
                }

                println("processing ${openBookings.size} open bookings ...")

                openBookings.forEach {
                    println("processing transfer of ${it.amount} cEuro with purpose ${it.purpose}")

                    try {
                        val confirmation1 = Confirmation(it.transferId, it.receiver, -it.amount, it.purpose)
                        tdbService.createNewTransaction(it.chain, it.sender, JSON.stringify(confirmation1), true)

                        val confirmation2 = Confirmation(it.transferId, it.sender, it.amount, it.purpose)
                        tdbService.createNewTransaction(it.chain, it.receiver, JSON.stringify(confirmation2), true)

                        processedTransferIds.add(it.transferId)
                    } catch (e: Exception) {
                        println("an exception was thrown (${e.message}), skipping this booking ...")
                    }
                }
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting application ...")
            }
        }
    }
}