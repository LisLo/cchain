package com.ismair.cchain

import com.ismair.cchain.keys.privateKeyPKCS8
import com.ismair.cchain.keys.publicKeyPKCS8
import com.ismair.cchain.securebase.*
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

        val publicKeyPKCS8WithoutNewLine = publicKeyPKCS8.replace("\n", " ")
        tdb.getChains(tdbSession).execute().extractList().forEach {
            val chainInfos = tdb.getTransactions(tdbSession!!, it.chain).execute().extractObj()
            if (chainInfos.count > 0) {
                val orders = chainInfos.transactions
                        .filter { it.receiver == publicKeyPKCS8WithoutNewLine }
                        .forEach {
                            val cryptKey = rsaCipher.decrypt(privateKey, it.cryptKey.replace(" ", ""))
                            try {
                                println("Encrypted Document: " + aesCipher.decrypt(cryptKey, it.document))
                            } catch (e: IllegalArgumentException) {
                                println("Document could not be decrypted")
                            }
                        }
            }
        }
    } catch (e: SecureBaseException) {
        println(e.message)
    }
}