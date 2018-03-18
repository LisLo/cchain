package com.ismair.cchain

import com.ismair.cchain.contracts.CashContract
import com.ismair.cchain.contracts.TradeContract
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.DefaultHelpFormatter
import com.xenomachina.argparser.mainBody
import de.transbase.cchain.extensions.toPrivateKey
import de.transbase.cchain.extensions.toPublicKey
import de.transbase.cchain.wrapper.TDBWrapper
import java.io.File
import java.security.PublicKey

enum class ContractType { CASH, TRADE }

class ArgumentClass(parser: ArgParser) {
    companion object {
        const val HELP_TEXT_TYPE = "contract type"
        const val HELP_TEXT_PUBLIC_KEY = "path to a base64 encoded pem file containing the public key"
        const val HELP_TEXT_PRIVATE_KEY = "path to a base64 encoded pem file containing the private key of the contract"
        const val HELP_TEXT_URL = "url to the transaction database"
        const val HELP_TEXT_USER = "username of the transaction database"
        const val HELP_TEXT_PASS = "password of the transaction database"
    }

    val contractType by parser.mapping(
            "--cash" to ContractType.CASH,
            "--trade" to ContractType.TRADE,
            help = HELP_TEXT_TYPE)
    val cashPub by parser.storing(HELP_TEXT_PUBLIC_KEY)
    val tradePub by parser.storing(HELP_TEXT_PUBLIC_KEY)
    val priv by parser.storing(HELP_TEXT_PRIVATE_KEY)
    val url by parser.storing(HELP_TEXT_URL)
    val user by parser.storing(HELP_TEXT_USER)
    val pass by parser.storing(HELP_TEXT_PASS)
}

fun readKey(path: String) = File(path).readText()

fun readPublicKey(path: String): Pair<String, PublicKey> {
    val publicKeyPKCS8 = readKey(path)
    return Pair(publicKeyPKCS8, publicKeyPKCS8.toPublicKey())
}

fun main(args: Array<String>) = mainBody {
    val info = "This application is able to run the smart contracts C-cash and C-trade"
    val parsedArgs = ArgParser(args, helpFormatter = DefaultHelpFormatter(info)).parseInto(::ArgumentClass)

    parsedArgs.run {
        val (cashPublicKeyPKCS8, cashPublicKey) = readPublicKey(cashPub)
        val (tradePublicKeyPKCS8, tradePublicKey) = readPublicKey(tradePub)
        val privateKey = readKey(priv).toPrivateKey()

        val contract = when (contractType) {
            ContractType.CASH -> {
                val tdbWrapper = TDBWrapper(cashPublicKey, cashPublicKeyPKCS8, privateKey, url, user, pass)
                CashContract(tdbWrapper, cashPublicKeyPKCS8, tradePublicKeyPKCS8)
            }
            ContractType.TRADE -> {
                val tdbWrapper = TDBWrapper(tradePublicKey, tradePublicKeyPKCS8, privateKey, url, user, pass)
                TradeContract(tdbWrapper, tradePublicKeyPKCS8, cashPublicKeyPKCS8)
            }
        }

        while (true) {
            try {
                contract.run()
            } catch (e: Exception) {
                println("an exception was thrown (${e.message}), restarting contract ...")
            }
        }
    }
}