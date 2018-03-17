package com.ismair.cchain.model.cheat

import com.ismair.cchain.abstracts.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class CheatConfirmation(
        override val requestId: Int,
        val mode: CheatMode
) : ContractResponse()