package com.ismair.cchain.securebase.extensions

import java.security.PublicKey
import java.util.*

// Regex + NO_WRAP necessary because WRAP encodes in RFC 2045 and not in RFC 1421
fun PublicKey.toPKCS8(): String {
    return "-----BEGIN PUBLIC KEY-----\n" +
            Regex(".{64}(?=.)").replace(Base64.getEncoder().encodeToString(encoded), "$0\n") + "\n" +
            "-----END PUBLIC KEY-----\n"
}