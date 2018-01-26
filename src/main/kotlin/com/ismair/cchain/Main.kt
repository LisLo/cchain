package com.ismair.cchain

import com.ismair.cchain.model.Booking
import com.ismair.cchain.model.Confirmation
import com.ismair.cchain.model.Transfer
import com.ismair.cchain.securebase.crypt.SecureBaseAESCipher
import com.ismair.cchain.securebase.crypt.SecureBaseRSACipher
import com.ismair.cchain.securebase.extensions.encodeURIComponent
import com.ismair.cchain.securebase.extensions.extractList
import com.ismair.cchain.securebase.extensions.toPrivateKey
import com.ismair.cchain.securebase.extensions.toPublicKey
import com.ismair.cchain.securebase.functions.prepareTransaction
import com.ismair.cchain.services.TDBService
import kotlinx.serialization.json.JSON
import java.io.File

fun main(args : Array<String>) {
    if (args.size != 5) {
        println("usage: java --jar CCash.jar [PUBLIC_KEY] [PRIVATE_KEY] [URL] [USER] [PASSWORD]")
        return
    }

    println("starting C-cash ...")

    val publicKeyPKCS8 = File(args[0]).readText()
    val publicKey = publicKeyPKCS8.toPublicKey()

    val privateKeyPKCS8 = File(args[1]).readText()
    val privateKey = privateKeyPKCS8.toPrivateKey()

    val tdbService = TDBService(args[2], args[3], args[4], publicKeyPKCS8, privateKey)

    println("loading all sent transactions ...")

    val processedTransferIds = mutableSetOf<Int>()
    val content = tdbService.getTransactionsBySender(publicKeyPKCS8.encodeURIComponent()).execute().extractList()
    content.forEach { chain ->
        if (chain.count > 0) {
            chain.transactions.forEach {
                try {
                    val encryptedCryptKey = SecureBaseRSACipher.decrypt(privateKey, it.cryptKeySender!!.replace(" ", ""))
                    val document = SecureBaseAESCipher.decrypt(encryptedCryptKey, it.document)
                    val confirmation = JSON.parse<Confirmation>(document)
                    processedTransferIds.add(confirmation.transferId)
                } catch (e: Exception) {
                    println("could not decrypt transaction with id = ${it.tid}")
                }
            }
        }
    }

    println("${processedTransferIds.size} transfers already processed")

    while (true) {
        try {
            println("loading all received transactions and calculating open transfers ...")

            val openBookings = mutableListOf<Booking>()
            val content2 = tdbService.getTransactionsByReceiver(publicKeyPKCS8.encodeURIComponent()).execute().extractList()
            content2.forEach { chain ->
                if (chain.count > 0) {
                    chain.transactions.forEach {
                        if (!processedTransferIds.contains(it.tid)) {
                            try {
                                val encryptedCryptKey = SecureBaseRSACipher.decrypt(privateKey, it.cryptKey!!.replace(" ", ""))
                                val document = SecureBaseAESCipher.decrypt(encryptedCryptKey, it.document)
                                val transfer = JSON.parse<Transfer>(document)
                                openBookings.add(Booking(it.tid, chain.chain, it.sender, transfer.receiver, transfer.amount, transfer.purpose))
                            } catch (e: Exception) {
                                println("could not decrypt transaction with id = ${it.tid}")
                            }
                        }
                    }
                }
            }

            println("processing ${openBookings.size} open bookings ...")

            openBookings.forEach {
                println("processing transfer of ${it.amount} cEuro with purpose ${it.purpose}")

                try {
                    val confirmation1 = Confirmation(it.transferId, it.receiver, -it.amount, it.purpose)
                    val message1 = JSON.stringify(confirmation1)
                    val transaction1 = prepareTransaction(it.chain, publicKey, privateKey, publicKeyPKCS8, it.sender, message1, true)
                    tdbService.createNewTransaction(transaction1).execute()

                    val confirmation2 = Confirmation(it.transferId, it.sender, it.amount, it.purpose)
                    val message2 = JSON.stringify(confirmation2)
                    val transaction2 = prepareTransaction(it.chain, publicKey, privateKey, publicKeyPKCS8, it.receiver, message2, true)
                    tdbService.createNewTransaction(transaction2).execute()

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