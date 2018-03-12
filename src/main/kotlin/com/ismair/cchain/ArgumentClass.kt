package com.ismair.cchain

import com.xenomachina.argparser.ArgParser

class ArgumentClass(parser: ArgParser) {
    val type by parser.mapping(
            "--cash" to ContractType.CASH,
            "--trade" to ContractType.TRADE,
            "--settle" to ContractType.SETTLE,
            help = "contract type")
    val pub by parser.storing("path to a base64 encoded pem file containing the public key")
    val priv by parser.storing("path to a base64 encoded pem file containing the private key")
    val url by parser.storing("url to the transaction database")
    val user by parser.storing("username of the transaction database")
    val pass by parser.storing("password of the transaction database")
}