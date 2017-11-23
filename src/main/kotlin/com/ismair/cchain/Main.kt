package com.ismair.cchain

import com.ismair.cchain.securebase.SecureBaseClient
import com.ismair.cchain.securebase.SecureBaseException
import com.ismair.cchain.securebase.TDB
import com.ismair.cchain.securebase.extractObj
import java.util.*

const val URL = "https://securebase.transbase.de:50443/REST/TDB/"
const val USER = "SecureBase2017"
const val PWD = "|NrBQF!ntpp'"
val tdb = SecureBaseClient(URL, USER, PWD, TDB::class.java).service

fun main(args : Array<String>) {
    println("C-cash started ...")

    var tdbSession: String? = null
    var tdbExpirationDate: Date? = null

    try {
        println("try to login ...")

        if (tdbSession == null || tdbExpirationDate == null || tdbExpirationDate.before(Date())) {
            val publicKey = ""//key.publicKeyPKCS8.encodeURIComponent()
            val randomToken = UUID.randomUUID().toString()
            val content = tdb.connect(randomToken).execute().extractObj()
            val loginToken = content.loginToken
            val signature = ""//key.getSignature(loginToken).encodeURIComponent()
            val content2 = tdb.login(TDB.Credentials(publicKey, loginToken, signature)).execute().extractObj()
            val calendar = Calendar.getInstance()
            calendar.time = content2.started
            calendar.add(Calendar.SECOND, content2.timeout)
            tdbExpirationDate = Date(calendar.timeInMillis)
            tdbSession = content2.session
        }
    } catch (e: SecureBaseException) {
        println(e.message)
    }
}