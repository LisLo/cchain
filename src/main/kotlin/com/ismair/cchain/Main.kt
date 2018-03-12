package com.ismair.cchain

import com.ismair.cchain.cash.CashContract
import com.ismair.cchain.settle.SettleContract
import com.ismair.cchain.trade.TradeContract
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import java.io.File

fun main(args: Array<String>) = mainBody {
    val parsedArgs = ArgParser(args).parseInto(::ArgumentClass)

    parsedArgs.run {
        val publicKeyPKCS8 = File(pub).readText()
        val privateKeyPKCS8 = File(priv).readText()

        when (type) {
            ContractType.CASH -> CashContract(publicKeyPKCS8, privateKeyPKCS8, url, user, pass)
            ContractType.TRADE -> TradeContract(publicKeyPKCS8, privateKeyPKCS8, url, user, pass)
            ContractType.SETTLE -> SettleContract(publicKeyPKCS8, privateKeyPKCS8, url, user, pass)
        }.run()
    }
}