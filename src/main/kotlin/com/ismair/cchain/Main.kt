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
import java.security.PrivateKey
import java.security.PublicKey

enum class ContractType { CASH, TRADE }

class ArgumentClass(parser: ArgParser) {
    companion object {
        const val HELP_TEXT_TYPE = "contract type"
        const val HELP_TEXT_KEY = "path to a base64 encoded pem file"
        const val HELP_TEXT_URL = "url to the transaction database"
        const val HELP_TEXT_USER = "username of the transaction database"
        const val HELP_TEXT_PASS = "password of the transaction database"
    }

    val contractType by parser.mapping(
            "--cash" to ContractType.CASH,
            "--trade" to ContractType.TRADE,
            help = HELP_TEXT_TYPE)
    val cashPub by parser.storing(HELP_TEXT_KEY)
    val cashPriv by parser.storing(HELP_TEXT_KEY)
    val tradePub by parser.storing(HELP_TEXT_KEY)
    val tradePriv by parser.storing(HELP_TEXT_KEY)
    val url by parser.storing(HELP_TEXT_URL)
    val user by parser.storing(HELP_TEXT_USER)
    val pass by parser.storing(HELP_TEXT_PASS)
}

fun readKey(path: String) = File(path).readText()

fun readPublicKey(path: String): Pair<String, PublicKey> {
    val publicKeyPKCS8 = readKey(path)
    return Pair(publicKeyPKCS8, publicKeyPKCS8.toPublicKey())
}

fun readPrivateKey(path: String): Pair<String, PrivateKey> {
    val privateKeyPKCS8 = readKey(path)
    return Pair(privateKeyPKCS8, privateKeyPKCS8.toPrivateKey())
}

fun main(args: Array<String>) = mainBody {
    val info = "This application is able to run the smart contracts C-cash and C-trade"
    val parsedArgs = ArgParser(args, helpFormatter = DefaultHelpFormatter(info)).parseInto(::ArgumentClass)

    parsedArgs.run {
        val (cashPublicKeyPKCS8, cashPublicKey) = readPublicKey(cashPub)
        val (_, cashPrivateKey) = readPrivateKey(cashPriv)
        val (tradePublicKeyPKCS8, tradePublicKey) = readPublicKey(tradePub)
        val (_, tradePrivateKey) = readPrivateKey(tradePriv)

        val contract = when (contractType) {
            ContractType.CASH -> {
                val tdbWrapper = TDBWrapper(cashPublicKey, cashPublicKeyPKCS8, cashPrivateKey, url, user, pass)
                CashContract(tdbWrapper, cashPublicKeyPKCS8, tradePublicKeyPKCS8)
            }
            ContractType.TRADE -> {
                val tdbWrapper = TDBWrapper(tradePublicKey, tradePublicKeyPKCS8, tradePrivateKey, url, user, pass)
                TradeContract(tdbWrapper, cashPublicKeyPKCS8)
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