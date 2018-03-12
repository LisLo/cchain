package com.ismair.cchain.cash

import com.ismair.cchain.Contract
import com.ismair.cchain.services.TDBService
import com.ismair.cchain.cash.model.Booking
import com.ismair.cchain.cash.model.Confirmation
import com.ismair.cchain.cash.model.Transfer
import de.transbase.cchain.crypt.SecureBaseAESCipher
import de.transbase.cchain.crypt.SecureBaseRSACipher
import de.transbase.cchain.extensions.encodeURIComponent
import de.transbase.cchain.extensions.extractList
import de.transbase.cchain.extensions.toPrivateKey
import de.transbase.cchain.extensions.toPublicKey
import de.transbase.cchain.functions.prepareTransaction
import kotlinx.serialization.json.JSON

class CashContract(
        private val publicKeyPKCS8: String,
        privateKeyPKCS8: String,
        url: String,
        username: String,
        password: String
) : Contract {
    private val publicKey = publicKeyPKCS8.toPublicKey()
    private val privateKey = privateKeyPKCS8.toPrivateKey()
    private val tdbService = TDBService(publicKeyPKCS8, privateKey, url, username, password)

    override fun run() {
        println("starting C-cash ...")

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
}