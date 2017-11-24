package com.ismair.cchain.securebase.crypt

import org.apache.commons.codec.binary.Hex
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecureBaseAESCipher {
    private val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    private val random = SecureRandom()

    private fun bytesToHex(arr: ByteArray) = String(Hex.encodeHex(arr))
    private fun hexToBytes(s: String) = Hex.decodeHex(s.toCharArray())

    fun encrypt(msg: String): Pair<String, String> {
        val secret = ByteArray(cipher.blockSize)
        random.nextBytes(secret)
        val secretKey = SecretKeySpec(secret, "AES")
        val iv = ByteArray(cipher.blockSize)
        random.nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val cryptKey = bytesToHex(secretKey.encoded) + "$" + bytesToHex(ivSpec.iv)
        val encryptedMsg = String(Base64.getMimeEncoder().encode(cipher.doFinal(msg.toByteArray())))
        return Pair(cryptKey, encryptedMsg)
    }

    fun decrypt(cryptKey: String, msg: String): String {
        val splitted = cryptKey.split('$')
        val secret = hexToBytes(splitted[0])
        val secretKey = SecretKeySpec(secret, "AES")
        val iv = hexToBytes(splitted[1])
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val tmp = Base64.getMimeDecoder().decode(msg)
        return String(cipher.doFinal(tmp))
    }
}