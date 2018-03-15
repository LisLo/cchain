package com.ismair.cchain.cash.model

import com.ismair.cchain.contract.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TransferConfirmation(
        override val requestId: Int,
        val request: TransferRequest,
        val mode: Mode
) : ContractResponse {
    enum class Mode { CREDIT, DEBIT }
}