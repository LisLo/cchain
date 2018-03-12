package com.ismair.cchain.settle

import com.ismair.cchain.Contract
import de.transbase.cchain.wrapper.TDBWrapper

class SettleContract(tdbWrapper: TDBWrapper) : Contract(tdbWrapper) {
    override fun run() {
        println("starting C-settle ...")

        println("not implemented yet")
    }
}