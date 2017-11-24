package com.ismair.cchain.securebase.crypt

import java.security.Key
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.*
import javax.crypto.Cipher

class SecureBaseRSACipher {
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    val signature = Signature.getInstance("SHA256withRSA")

    private fun crypt(key: Key, message: ByteArray, mode: Int): ByteArray {
        cipher.init(mode, key)
        return cipher.doFinal(message)
    }

    fun encrypt(publicKey: PublicKey, message: String): String {
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.getEncoder().encodeToString(cipher.doFinal(message.toByteArray()))
    }

    fun decrypt(privateKey: PrivateKey, message: String): String {
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return String(cipher.doFinal(Base64.getDecoder().decode(message.toByteArray())))
    }

    fun sign(privateKey: PrivateKey, message: String): String {
        signature.initSign(privateKey)
        signature.update(message.toByteArray())
        val signature = signature.sign()
        return Base64.getEncoder().encodeToString(signature)
    }
}