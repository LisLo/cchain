package com.ismair.cchain.securebase.extensions

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

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

fun String.encodeBase64() = Base64.getEncoder().encodeToString(this.toByteArray())

fun String.decodeBase64() = String(Base64.getDecoder().decode(this.toByteArray()))

fun String.toPublicKey(): PublicKey {
    val stripped = this
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace(" ", "")
    val encoded = Base64.getDecoder().decode(stripped)
    val keySpec = X509EncodedKeySpec(encoded)
    val kf = KeyFactory.getInstance("RSA")
    return kf.generatePublic(keySpec)
}

fun String.toPrivateKey(): PrivateKey {
    val stripped = this
            .replace("-----BEGIN PRIVATE KEY-----\n", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
    val encoded = Base64.getDecoder().decode(stripped)
    val keySpec = PKCS8EncodedKeySpec(encoded)
    val kf = KeyFactory.getInstance("RSA")
    return kf.generatePrivate(keySpec)
}