package com.ismair.cchain.securebase.extensions

import org.apache.commons.codec.binary.Base64
import java.security.PublicKey

// Regex + NO_WRAP necessary because WRAP encodes in RFC 2045 and not in RFC 1421
fun PublicKey.toPKCS8(): String {
    return "-----BEGIN PUBLIC KEY-----\n" +
            Regex(".{64}(?=.)").replace(String(Base64.encodeBase64(encoded)), "$0\n") + "\n" +
            "-----END PUBLIC KEY-----\n"
}