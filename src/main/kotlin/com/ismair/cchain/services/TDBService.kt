package com.ismair.cchain.services

import de.transbase.cchain.TDB
import de.transbase.cchain.crypt.SecureBaseRSACipher
import de.transbase.cchain.extensions.encodeURIComponent
import de.transbase.cchain.extensions.extractObj
import de.transbase.cchain.functions.createSecureBaseService
import java.security.PrivateKey
import java.util.*

class TDBService(url: String, user: String, password: String, val publicKeyPKCS8: String, val privateKey: PrivateKey) {
    private val tdb = createSecureBaseService(url, user, password, TDB::class.java)
    private var tdbSession: String? = null
    private var tdbExpirationDate: Date? = null

    private val session: String
        get() {
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

    fun getTransactionsBySender(sender: String) =
            tdb.getTransactions(session, "", sender, "")

    fun getTransactionsByReceiver(receiver: String) =
            tdb.getTransactions(session, "", "", receiver)

    fun createNewTransaction(transaction: TDB.Transaction) =
            tdb.createNewTransaction(session, transaction)
}