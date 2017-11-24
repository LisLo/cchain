package com.ismair.cchain

import com.ismair.cchain.keys.privateKeyPKCS8
import com.ismair.cchain.keys.publicKeyPKCS8
import com.ismair.cchain.securebase.*
import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import java.util.*

@Serializable
data class Contract(val receiver: String, val amount: Int, val purpose: String)

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

        val contracts = mutableListOf<Pair<Int, Contract>>()
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
                                contracts.add(Pair(it.tid, JSON.parse<Contract>(message)))
                            } catch (e: Exception) {
                                println("transaction with id = " + it.tid + " in chain " + chainInfos.chain + " could not be read")                            }
                        }
            }
        }

        println("transactions were successfully read, there are " + contracts.size + " open contracts")

        // TODO
    } catch (e: SecureBaseException) {
        println(e.message)
    }
}