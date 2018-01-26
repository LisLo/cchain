package com.ismair.cchain.securebase.crypt

import org.apache.commons.codec.binary.Base64
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import javax.crypto.Cipher

object SecureBaseRSACipher {
    private val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    private val signature = Signature.getInstance("SHA256withRSA")

    fun encrypt(publicKey: PublicKey, message: String): String {
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return String(Base64.encodeBase64(cipher.doFinal(message.toByteArray())))
    }

    fun decrypt(privateKey: PrivateKey, message: String): String {
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return String(cipher.doFinal(Base64.decodeBase64(message.toByteArray())))
    }

    fun sign(privateKey: PrivateKey, message: String): String {
        signature.initSign(privateKey)
        signature.update(message.toByteArray())
        val signature = signature.sign()
        return String(Base64.encodeBase64(signature))
    }

    fun verify(publicKey: PublicKey, sign: String, message: String): Boolean {
        signature.initVerify(publicKey)
        signature.update(message.toByteArray())
        return signature.verify(Base64.decodeBase64(sign.toByteArray()))
    }
}