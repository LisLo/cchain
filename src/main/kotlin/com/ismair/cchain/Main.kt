package com.ismair.cchain

import com.ismair.cchain.keys.privateKeyPKCS8
import com.ismair.cchain.keys.publicKeyPKCS8
import com.ismair.cchain.model.Booking
import com.ismair.cchain.model.Confirmation
import com.ismair.cchain.model.Transfer
import com.ismair.cchain.securebase.SecureBaseClient
import com.ismair.cchain.securebase.SecureBaseException
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
        val tdbExpirationDate = Date(calendar.timeInMillis)
        val tdbSession = content2.session

        println("login was successful: " + tdbSession + " until " + tdbExpirationDate)

        val transfers = mutableListOf<Booking>()
        var countMistakes = 0
        val publicKeyPKCS8WithoutNewLine = publicKeyPKCS8.replace("\n", " ")

        tdb.getChains(tdbSession).execute().extractList().forEach {
            val chainInfos = tdb.getTransactions(tdbSession, it.chain).execute().extractObj()
            if (chainInfos.count > 0) {
                chainInfos.transactions
                        .filter { it.receiver == publicKeyPKCS8WithoutNewLine }
                        .forEach {
                            try {
                                val cryptKey = rsaCipher.decrypt(privateKey, it.cryptKey.replace(" ", ""))
                                val message = aesCipher.decrypt(cryptKey, it.document)
                                val transfer = JSON.parse<Transfer>(message)
                                transfers.add(Booking(it.tid, chainInfos.chain, it.sender, transfer.receiver, transfer.amount, transfer.purpose))
                            } catch (e: Exception) {
                                ++countMistakes
                            }
                        }
            }
        }

        println("transactions were successfully read, there are " + transfers.size + " open bookings")
        if (countMistakes > 0) {
            println("warning: " + countMistakes + " messages could not be parsed")
        }

        transfers.forEach {
            println("transfer of " + it.amount + " with purpose " + it.purpose)

            val confirmation = Confirmation(it.id, it.sender, it.amount, it.purpose)
            val pair = aesCipher.encrypt(JSON.stringify(confirmation))
            val cryptKey = rsaCipher.encrypt(it.receiver.toPublicKey(), pair.first)
            val document = pair.second
            val signature = rsaCipher.sign(privateKey, document)

            tdb.createNewTransaction(tdbSession, TDB.Transaction(
                    it.chain,
                    publicKeyPKCS8.encodeURIComponent(),
                    it.receiver.encodeURIComponent(),
                    document.encodeURIComponent(),
                    cryptKey.encodeURIComponent(),
                    signature.encodeURIComponent())).execute()
        }
    } catch (e: SecureBaseException) {
        println(e.message)
    }
}