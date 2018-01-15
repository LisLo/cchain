package com.ismair.cchain

import com.ismair.cchain.keys.privateKeyPKCS8
import com.ismair.cchain.keys.publicKeyPKCS8
import com.ismair.cchain.model.Booking
import com.ismair.cchain.model.Confirmation
import com.ismair.cchain.model.Transfer
import com.ismair.cchain.securebase.TDB
import com.ismair.cchain.securebase.crypt.SecureBaseAESCipher
import com.ismair.cchain.securebase.crypt.SecureBaseRSACipher
import com.ismair.cchain.securebase.extensions.*
import com.ismair.cchain.securebase.functions.createSecureBaseService
import com.ismair.cchain.securebase.functions.prepareTransaction
import kotlinx.serialization.json.JSON
import java.util.*

const val URL = "https://securebase.transbase.de:50443/REST/TDB/"
const val USER = "SecureBase2017"
const val PWD = "|NrBQF!ntpp'"
val tdb = createSecureBaseService(URL, USER, PWD, TDB::class.java)
var tdbSession: String? = null
var tdbExpirationDate: Date? = null

val publicKey = publicKeyPKCS8.toPublicKey()
val privateKey = privateKeyPKCS8.toPrivateKey()

fun getSession(): String {
    var session = tdbSession
    var expirationDate = tdbExpirationDate

    while (session == null || expirationDate == null || expirationDate.before(Date())) {
        println("trying to login to tdb ...")

        try {
            val publicKey = publicKeyPKCS8.encodeURIComponent()
            val randomToken = UUID.randomUUID().toString()
            val content = tdb.connect(randomToken).execute().extractObj()
            val loginToken = content.loginToken
            val signature = SecureBaseRSACipher.sign(privateKey, loginToken).encodeURIComponent()
            val content2 = tdb.login(TDB.Credentials(publicKey, loginToken, signature)).execute().extractObj()
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.SECOND, content2.timeout)
            session = content2.session
            expirationDate = Date(calendar.timeInMillis)

            println("login was successful: $session until $expirationDate")
        } catch (e: Exception) {
            println("login failed with an exception: " + e.message)
        }
    }

    tdbSession = session
    tdbExpirationDate = expirationDate
    return session
}

fun main(args : Array<String>) {
    println("starting C-cash ...")

    println("loading all send transactions ...")

    val processedTransferIds = mutableSetOf<Int>()
    val content = tdb.getTransactions(getSession(), "", publicKeyPKCS8.encodeURIComponent(), "").execute().extractList()
    content.forEach { chain ->
        if (chain.count > 0) {
            chain.transactions.forEach {
                try {
                    val encryptedCryptKey = SecureBaseRSACipher.decrypt(privateKey, it.cryptKeySender.replace(" ", ""))
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
            val content2 = tdb.getTransactions(getSession(), "", "", publicKeyPKCS8.encodeURIComponent()).execute().extractList()
            content2.forEach { chain ->
                if (chain.count > 0) {
                    chain.transactions.forEach {
                        if (!processedTransferIds.contains(it.tid)) {
                            try {
                                val encryptedCryptKey = SecureBaseRSACipher.decrypt(privateKey, it.cryptKey.replace(" ", ""))
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
                    tdb.createNewTransaction(getSession(), transaction1).execute()

                    val confirmation2 = Confirmation(it.transferId, it.sender, it.amount, it.purpose)
                    val message2 = JSON.stringify(confirmation2)
                    val transaction2 = prepareTransaction(it.chain, publicKey, privateKey, publicKeyPKCS8, it.receiver, message2, true)
                    tdb.createNewTransaction(getSession(), transaction2).execute()

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