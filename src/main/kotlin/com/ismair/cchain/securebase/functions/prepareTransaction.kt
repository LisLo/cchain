package com.ismair.cchain.securebase.functions

import com.ismair.cchain.securebase.TDB
import com.ismair.cchain.securebase.crypt.SecureBaseAESCipher
import com.ismair.cchain.securebase.crypt.SecureBaseRSACipher
import com.ismair.cchain.securebase.extensions.encodeBase64
import com.ismair.cchain.securebase.extensions.encodeURIComponent
import com.ismair.cchain.securebase.extensions.toPublicKey
import java.security.PrivateKey
import java.security.PublicKey

fun prepareTransaction(chain: String, publicKey: PublicKey, privateKey: PrivateKey, sender: String,
                       receiver: String, message: String, encrypted: Boolean): TDB.Transaction {
    var document = ""
    var cryptKey = ""
    var cryptKeySender = ""
    if (encrypted) {
        val pair = SecureBaseAESCipher.encrypt(message)
        cryptKey = SecureBaseRSACipher.encrypt(receiver.toPublicKey(), pair.first)
        cryptKeySender = SecureBaseRSACipher.encrypt(publicKey, pair.first)
        document = pair.second
    } else {
        document = message.encodeBase64()
    }
    val signature = SecureBaseRSACipher.sign(privateKey, document)

    return TDB.Transaction(
            chain,
            sender.encodeURIComponent(),
            receiver.encodeURIComponent(),
            document.encodeURIComponent(),
            cryptKey.encodeURIComponent(),
            cryptKeySender.encodeURIComponent(),
            signature.encodeURIComponent())
}