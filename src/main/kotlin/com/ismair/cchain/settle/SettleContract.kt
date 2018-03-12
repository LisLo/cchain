package com.ismair.cchain.settle

import com.ismair.cchain.Contract
import com.ismair.cchain.services.TDBService
import de.transbase.cchain.extensions.toPrivateKey
import de.transbase.cchain.extensions.toPublicKey

class SettleContract(
        private val publicKeyPKCS8: String,
        privateKeyPKCS8: String,
        url: String,
        username: String,
        password: String
) : Contract {
    private val publicKey = publicKeyPKCS8.toPublicKey()
    private val privateKey = privateKeyPKCS8.toPrivateKey()
    private val tdbService = TDBService(publicKeyPKCS8, privateKey, url, username, password)

    override fun run() {
        println("starting C-settle ...")

        println("not implemented yet")
    }
}