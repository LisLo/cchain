package com.ismair.cchain

import com.ismair.cchain.contracts.ContractType
import com.ismair.cchain.contracts.cash.CashContract
import com.ismair.cchain.contracts.trade.TradeContract
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import de.transbase.cchain.extensions.toPrivateKey
import de.transbase.cchain.extensions.toPublicKey
import de.transbase.cchain.wrapper.TDBWrapper
import java.io.File

class ArgumentClass(parser: ArgParser) {
    val contractType by parser.mapping(
            "--cash" to ContractType.CASH,
            "--trade" to ContractType.TRADE,
            help = "contracts type")
    val pub by parser.storing("path to a base64 encoded pem file containing the public key")
    val priv by parser.storing("path to a base64 encoded pem file containing the private key")
    val url by parser.storing("url to the transaction database")
    val user by parser.storing("username of the transaction database")
    val pass by parser.storing("password of the transaction database")
}

fun main(args: Array<String>) = mainBody {
    val parsedArgs = ArgParser(args).parseInto(::ArgumentClass)

    parsedArgs.run {
        val publicKeyPKCS8 = File(pub).readText()
        val publicKey = publicKeyPKCS8.toPublicKey()
        val privateKeyPKCS8 = File(priv).readText()
        val privateKey = privateKeyPKCS8.toPrivateKey()
        val tdbWrapper = TDBWrapper(publicKey, publicKeyPKCS8, privateKey, url, user, pass)
        val contract = when (contractType) {
            ContractType.CASH -> CashContract(tdbWrapper)
            ContractType.TRADE -> TradeContract(tdbWrapper)
        }

        while (true) {
            try {
                contract.run()
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting contracts ...")
            }
        }
    }
}