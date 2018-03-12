package com.ismair.cchain.settle

import com.ismair.cchain.Contract
import com.ismair.cchain.services.TDBService

class SettleContract(tdbService: TDBService) : Contract(tdbService) {
    override fun run() {
        println("starting C-settle ...")

        println("not implemented yet")
    }
}