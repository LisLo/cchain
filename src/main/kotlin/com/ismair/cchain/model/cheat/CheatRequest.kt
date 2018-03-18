package com.ismair.cchain.model.cheat

import com.ismair.cchain.model.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class CheatRequest(
        val mode: CheatMode
) : ContractRequest()