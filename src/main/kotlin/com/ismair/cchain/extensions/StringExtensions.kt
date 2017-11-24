package com.ismair.cchain.extensions

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

fun String.encodeURIComponent(): String {
    var result: String

    try {
        result = URLEncoder.encode(this, "UTF-8")
                .replace("\\+".toRegex(), "%20")
                .replace("\\%21".toRegex(), "!")
                .replace("\\%27".toRegex(), "'")
                .replace("\\%28".toRegex(), "(")
                .replace("\\%29".toRegex(), ")")
                .replace("\\%7E".toRegex(), "~")
    } catch (e: UnsupportedEncodingException) {
        result = this
    }

    return result
}

fun String.toPublicKey(): PublicKey {
    val stripped = this
            .replace("-----BEGIN PUBLIC KEY-----\n", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
    val encoded = Base64.getDecoder().decode(stripped)
    val keySpec = X509EncodedKeySpec(encoded)
    val kf = KeyFactory.getInstance("RSA", "SC")
    return kf.generatePublic(keySpec)
}

fun String.toPrivateKey(): PrivateKey {
    val stripped = this
            .replace("-----BEGIN PRIVATE KEY-----\n", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
    val encoded = Base64.getDecoder().decode(stripped)
    val keySpec = PKCS8EncodedKeySpec(encoded)
    val kf = KeyFactory.getInstance("RSA", "SC")
    return kf.generatePrivate(keySpec)
}

fun crypt(key: Key, message: ByteArray, mode: Int): ByteArray {
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    cipher.init(mode, key)
    return cipher.doFinal(message)
}

fun String.encrypt(publicKey: PublicKey)
        = Base64.getEncoder().encodeToString(crypt(publicKey, this.toByteArray(), Cipher.ENCRYPT_MODE))

fun String.decrypt(privateKey: PrivateKey)
        = String(crypt(privateKey, Base64.getDecoder().decode(this.toByteArray()), Cipher.DECRYPT_MODE))

fun String.sign(privateKey: PrivateKey): String {
    val privateSignature = Signature.getInstance("SHA256withRSA")
    privateSignature.initSign(privateKey)
    privateSignature.update(this.toByteArray())
    val signature = privateSignature.sign()
    return Base64.getEncoder().encodeToString(signature)
}