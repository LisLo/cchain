package com.ismair.cchain.contract

import de.transbase.cchain.wrapper.TDBWrapper

abstract class Contract {
    abstract fun run(tdbWrapper: TDBWrapper)
}