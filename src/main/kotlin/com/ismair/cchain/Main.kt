package com.ismair.cchain

import com.ismair.cchain.keys.privateKeyPKCS8
import com.ismair.cchain.keys.publicKeyPKCS8
import com.ismair.cchain.model.Booking
import com.ismair.cchain.model.Confirmation
import com.ismair.cchain.model.Transfer
import com.ismair.cchain.securebase.SecureBaseClient
import com.ismair.cchain.securebase.TDB
import com.ismair.cchain.securebase.crypt.SecureBaseAESCipher
import com.ismair.cchain.securebase.crypt.SecureBaseRSACipher
import com.ismair.cchain.securebase.extensions.*
import kotlinx.serialization.json.JSON
import java.util.*

fun main(args : Array<String>) {
    println("starting C-cash ...")

    println("initializing tdb service ...")

    val URL = "https://securebase.transbase.de:50443/REST/TDB/"
    val USER = "SecureBase2017"
    val PWD = "|NrBQF!ntpp'"
    val tdb = SecureBaseClient(URL, USER, PWD, TDB::class.java).service

    println("initializing cryptographic libraries ...")

    val publicKey = publicKeyPKCS8.toPublicKey()
    val privateKey = privateKeyPKCS8.toPrivateKey()
    val rsaCipher = SecureBaseRSACipher()
    val aesCipher = SecureBaseAESCipher()

    println("starting main loop ...")

    var tdbSession: String? = null
    var tdbExpirationDate: Date? = null

    while (true) {
        val session = tdbSession
        if (session == null || tdbExpirationDate == null || tdbExpirationDate.before(Date())) {
            println("trying to login to tdb ...")

            try {
                val pubKey = publicKeyPKCS8.encodeURIComponent()
                val randomToken = UUID.randomUUID().toString()
                val content = tdb.connect(randomToken).execute().extractObj()
                val loginToken = content.loginToken
                val signature = rsaCipher.sign(privateKey, loginToken).encodeURIComponent()
                val content2 = tdb.login(TDB.Credentials(pubKey, loginToken, signature)).execute().extractObj()
                val calendar = Calendar.getInstance()
                calendar.time = content2.started
                calendar.add(Calendar.SECOND, content2.timeout)
                tdbExpirationDate = Date(calendar.timeInMillis)
                tdbSession = content2.session

                println("login was successful: " + tdbSession + " until " + tdbExpirationDate)
            } catch (e: Exception) {
                println("login failed with an exception: " + e.message)
            }
        } else {
            println("loading all transactions ...")

            val allTransactions = mutableListOf<Pair<String, TDB.TransactionInfoContent>>()

            tdb.getChains(session).execute().extractList().forEach {
                val chainInfos = tdb.getTransactions(session, it.chain).execute().extractObj()
                if (chainInfos.count > 0) {
                    allTransactions.addAll(chainInfos.transactions.map { Pair(chainInfos.chain, it) })
                }
            }

            println("calculating open transfers ...")

            val publicKeyPKCS8WithoutNewLine = publicKeyPKCS8.replace("\n", " ")
            val processedTransferIds = mutableListOf<Int>()
            val openBookings = mutableListOf<Booking>()

            allTransactions
                    .filter { it.second.sender == publicKeyPKCS8WithoutNewLine && !it.second.cryptKeySender.isNullOrEmpty() }
                    .forEach {
                        try {
                            val encryptedCryptKey = rsaCipher.decrypt(privateKey, it.second.cryptKeySender!!.replace(" ", ""))
                            val document = aesCipher.decrypt(encryptedCryptKey, it.second.document)
                            val confirmation = JSON.parse<Confirmation>(document)
                            processedTransferIds.add(confirmation.transferId)
                        } catch (e: Exception) {}
                    }

            println(processedTransferIds.size.toString() + " transfers already processed")

            allTransactions
                    .filter { it.second.receiver == publicKeyPKCS8WithoutNewLine && !it.second.cryptKey.isNullOrEmpty() && !processedTransferIds.contains(it.second.tid) }
                    .forEach {
                        try {
                            val encryptedCryptKey = rsaCipher.decrypt(privateKey, it.second.cryptKey!!.replace(" ", ""))
                            val document = aesCipher.decrypt(encryptedCryptKey, it.second.document)
                            val transfer = JSON.parse<Transfer>(document)
                            openBookings.add(Booking(it.second.tid, it.first, it.second.sender, transfer.receiver, transfer.amount, transfer.purpose))
                        } catch (e: Exception) {}
                    }

            println("processing " + openBookings.size + " open bookings ...")

            openBookings.forEach {
                println("processing transfer of " + it.amount + "€ with purpose " + it.purpose)

                val confirmation1 = Confirmation(it.transferId, it.sender, it.amount, it.purpose)
                val pair1 = aesCipher.encrypt(JSON.stringify(confirmation1))
                val cryptKey1 = rsaCipher.encrypt(it.receiver.toPublicKey(), pair1.first)
                val cryptKeySender1 = rsaCipher.encrypt(publicKey, pair1.first)
                val document1 = pair1.second
                val signature1 = rsaCipher.sign(privateKey, document1)

                tdb.createNewTransaction(session, TDB.Transaction(
                        it.chain,
                        publicKeyPKCS8.encodeURIComponent(),
                        it.receiver.encodeURIComponent(),
                        document1.encodeURIComponent(),
                        cryptKey1.encodeURIComponent(),
                        cryptKeySender1.encodeURIComponent(),
                        signature1.encodeURIComponent())).execute()

                val confirmation2 = Confirmation(it.transferId, it.receiver, -it.amount, it.purpose)
                val pair2 = aesCipher.encrypt(JSON.stringify(confirmation2))
                val cryptKey2 = rsaCipher.encrypt(it.sender.toPublicKey(), pair2.first)
                val cryptKeySender2 = rsaCipher.encrypt(publicKey, pair2.first)
                val document2 = pair2.second
                val signature2 = rsaCipher.sign(privateKey, document2)

                tdb.createNewTransaction(session, TDB.Transaction(
                        it.chain,
                        publicKeyPKCS8.encodeURIComponent(),
                        it.sender.encodeURIComponent(),
                        document2.encodeURIComponent(),
                        cryptKey2.encodeURIComponent(),
                        cryptKeySender2.encodeURIComponent(),
                        signature2.encodeURIComponent())).execute()
            }
        }
    }
}