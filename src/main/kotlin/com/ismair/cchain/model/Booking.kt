package com.ismair.cchain.model

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "booking")
class Booking() {
    @DatabaseField(id = true) var id: Int = 0
    @DatabaseField var chain: String = ""
    @DatabaseField var sender: String = ""
    @DatabaseField var receiver: String = ""
    @DatabaseField var amount: Int = 0
    @DatabaseField var purpose: String = ""

    constructor(id: Int, chain: String, sender: String, receiver: String, amount: Int, purpose: String) : this() {
        this.id = id
        this.chain = chain
        this.sender = sender
        this.receiver = receiver
        this.amount = amount
        this.purpose = purpose
    }
}