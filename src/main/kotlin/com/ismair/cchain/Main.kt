package com.ismair.cchain

import com.ismair.cchain.cash.CashContract
import com.ismair.cchain.services.TDBService
import com.ismair.cchain.settle.SettleContract
import com.ismair.cchain.trade.TradeContract
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import de.transbase.cchain.extensions.toPrivateKey
import de.transbase.cchain.extensions.toPublicKey
import java.io.File

fun main(args: Array<String>) = mainBody {
    val parsedArgs = ArgParser(args).parseInto(::ArgumentClass)

    parsedArgs.run {
        val publicKeyPKCS8 = File(pub).readText()
        val publicKey = publicKeyPKCS8.toPublicKey()
        val privateKeyPKCS8 = File(priv).readText()
        val privateKey = privateKeyPKCS8.toPrivateKey()
        val tdbService = TDBService(publicKey, publicKeyPKCS8, privateKey, url, user, pass)

        when (type) {
            ContractType.CASH -> CashContract(tdbService)
            ContractType.TRADE -> TradeContract(tdbService)
            ContractType.SETTLE -> SettleContract(tdbService)
        }.run()
    }
}