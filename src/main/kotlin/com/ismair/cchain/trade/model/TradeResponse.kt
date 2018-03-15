package com.ismair.cchain.trade.model

interface TradeResponse {
    val requestId: Int
    val request: TradeRequest
}