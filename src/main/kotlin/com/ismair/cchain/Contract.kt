package com.ismair.cchain

import com.ismair.cchain.services.TDBService

abstract class Contract(protected val tdbService: TDBService) {
    abstract fun run()
}