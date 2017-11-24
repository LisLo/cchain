package com.ismair.cchain.securebase

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

fun String.toPublicKey(): PublicKey {
    val stripped = this
            .replace("-----BEGIN PUBLIC KEY-----\n", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
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