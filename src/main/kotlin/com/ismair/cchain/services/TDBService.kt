package com.ismair.cchain.services

import de.transbase.cchain.TDB
import de.transbase.cchain.crypt.SecureBaseAESCipher
import de.transbase.cchain.crypt.SecureBaseRSACipher
import de.transbase.cchain.extensions.decodeBase64
import de.transbase.cchain.extensions.encodeURIComponent
import de.transbase.cchain.extensions.extractList
import de.transbase.cchain.extensions.extractObj
import de.transbase.cchain.functions.createSecureBaseService
import de.transbase.cchain.functions.prepareTransaction
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*

class TDBService(
        private val publicKey: PublicKey,
        private val publicKeyPKCS8: String,
        private val privateKey: PrivateKey,
        url: String,
        username: String,
        password: String
) {
    private val tdb = createSecureBaseService(url, username, password, TDB::class.java)
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

    private val originalDocuments = mutableMapOf<Int, String>()
    private val meShrinked = shrink(publicKeyPKCS8)

    private fun shrink(publicKey: String) =
            publicKey.replace("\n", "").replace("\r", "").replace(" ", "")

    private fun isMe(publicKey: String) = shrink(publicKey) == meShrinked

    data class DecryptedTransaction(
            val chain: String,
            val id: Int,
            val sender: String,
            val receiver: String,
            val document: String?
    )

    private fun decrypt(content: List<TDB.TransactionInfosContent>) =
            content.flatMap { chain ->
                if (chain.count > 0) {
                    chain.transactions.map {
                        val document = if (originalDocuments.containsKey(it.tid)) {
                            originalDocuments[it.tid]
                        } else {
                            val isSender = isMe(it.sender)
                            val isReceiver = isMe(it.receiver)

                            if (it.cryptKey.isNullOrEmpty()) {
                                it.document.decodeBase64()
                            } else if (isSender || isReceiver) {
                                try {
                                    val key = if (isSender) it.cryptKeySender else it.cryptKey
                                    if (key != null) {
                                        val encryption = SecureBaseAESCipher.decrypt(SecureBaseRSACipher.decrypt(privateKey, key), it.document)
                                        originalDocuments[it.tid] = encryption
                                        encryption
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            } else {
                                null
                            }
                        }

                        DecryptedTransaction(chain.chain, it.tid, it.sender, it.receiver, document)
                    }
                } else {
                    listOf()
                }
            }

    fun getTransactionsByChain(chain: String) =
            decrypt(tdb.getTransactions(session, chain, "", "").execute().extractList())

    fun getSentTransactions() =
            decrypt(tdb.getTransactions(session, "", publicKeyPKCS8.encodeURIComponent(), "").execute().extractList())

    fun getReceivedTransactions() =
            decrypt(tdb.getTransactions(session, "", "", publicKeyPKCS8.encodeURIComponent()).execute().extractList())

    fun createChainIfNotExists(name: String, description: String) {
        if (!tdb.getChains(session).execute().extractList().any { it.chain == name }) {
            tdb.createNewChain(session, TDB.Chain(name, description)).execute()
        }
    }

    fun createNewTransaction(chain: String, receiver: String, message: String, encrypted: Boolean) {
        createChainIfNotExists(chain, "created automatically")
        val transaction = prepareTransaction(chain, publicKey, privateKey, publicKeyPKCS8, receiver, message, encrypted)
        tdb.createNewTransaction(session, transaction).execute()
    }
}