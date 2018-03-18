package com.ismair.cchain.model

abstract class ContractResponse : ContractDocument() {
    abstract val requestId: Int
}