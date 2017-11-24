package com.ismair.cchain

import com.ismair.cchain.extensions.encodeURIComponent
import com.ismair.cchain.extensions.sign
import com.ismair.cchain.extensions.toPrivateKey
import com.ismair.cchain.extensions.toPublicKey
import com.ismair.cchain.keys.privateKeyPKCS8
import com.ismair.cchain.keys.publicKeyPKCS8
import com.ismair.cchain.securebase.*
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.*

const val URL = "https://securebase.transbase.de:50443/REST/TDB/"
const val USER = "SecureBase2017"
const val PWD = "|NrBQF!ntpp'"
val tdb = SecureBaseClient(URL, USER, PWD, TDB::class.java).service

fun main(args : Array<String>) {
    println("starting C-cash ...")

    Security.addProvider(BouncyCastleProvider())

    println("loading private and public key ...")

    val publicKey = publicKeyPKCS8.toPublicKey()
    val privateKey = privateKeyPKCS8.toPrivateKey()

    println("trying to login ...")

    var tdbSession: String? = null
    var tdbExpirationDate: Date? = null

    try {
        if (tdbSession == null || tdbExpirationDate == null || tdbExpirationDate.before(Date())) {
            val pubKey = publicKeyPKCS8.encodeURIComponent()
            val randomToken = UUID.randomUUID().toString()
            val content = tdb.connect(randomToken).execute().extractObj()
            val loginToken = content.loginToken
            val signature = loginToken.sign(privateKey).encodeURIComponent()
            val content2 = tdb.login(TDB.Credentials(pubKey, loginToken, signature)).execute().extractObj()
            val calendar = Calendar.getInstance()
            calendar.time = content2.started
            calendar.add(Calendar.SECOND, content2.timeout)
            tdbExpirationDate = Date(calendar.timeInMillis)
            tdbSession = content2.session

            println("login was successful: " + tdbSession + " " + tdbExpirationDate)

            tdb.getChains(tdbSession).execute().extractList().forEach { println(it.chain) }
        }
    } catch (e: SecureBaseException) {
        println(e.message)
    }
}