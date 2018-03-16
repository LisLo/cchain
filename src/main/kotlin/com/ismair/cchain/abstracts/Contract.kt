package com.ismair.cchain.abstracts

import de.transbase.cchain.wrapper.TDBWrapper

abstract class Contract(protected val tdbWrapper: TDBWrapper) {
    abstract fun run()
}