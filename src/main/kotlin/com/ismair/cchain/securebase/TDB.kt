package com.ismair.cchain.securebase

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*
import java.util.Date

interface TDB {
    data class Credentials(
            @SerializedName("public_key") val publicKey: String,
            @SerializedName("login_token") val loginToken: String,
            val signature: String
    )

    data class Chain(
            val chain: String,
            val description: String
    )

    data class Transaction(
            val chain: String,
            val sender: String,
            val receiver: String,
            val document: String,
            @SerializedName("cryptkey") val cryptKey: String?,
            @SerializedName("cryptkey_sender") val cryptKeySender: String?,
            val signature: String
    )

    data class ConnectContent(
            @SerializedName("public_key") val publicKey: String,
            @SerializedName("connect_token") val connectToken: String,
            val signature: String,
            @SerializedName("login_token") val loginToken: String
    )

    data class SessionContent(
            val session: String,
            val started: Date,
            val timeout: Int
    )

    data class ChainContent(
            val chain: String,
            val cid: Int,
            val status: Int
    )

    data class ChainCreatedContent(
            val chain: String,
            val cid: Int,
            val status: Int,
            val created: Date
    )

    data class ChainFullContent(
            val chain: String,
            val description: String,
            val status: Int,
            val created: Date,
            val modified: Date,
            val cid: Int,
            val owner: String
    )

    data class TransactionCreatedContent(
            val tid: Int,
            val status: Int,
            val created: Date,
            val cid: Int
    )

    data class TransactionInfoContent(
            val tid: Int,
            val created: Date,
            val sender: String,
            val receiver: String,
            val document: String,
            @SerializedName("cryptkey") val cryptKey: String?,
            @SerializedName("cryptkey_sender") val cryptKeySender: String?,
            val signature: String
    )

    data class TransactionInfosContent(
            val chain: String,
            val cid: Int,
            val count: Int,
            @SerializedName("transaction") val transactions: Array<TransactionInfoContent>
    )

    @GET("user/connect")
    fun connect(@Query("connect_token") connectToken: String): Call<SecureBaseResponse<ConnectContent>>

    @PUT("user/login")
    fun login(@Body credentials: Credentials): Call<SecureBaseResponse<SessionContent>>

    @DELETE("user/logout")
    fun logout(@Header("session") session: String): Call<SecureBaseResponse<SessionContent>>

    @PUT("chain/new")
    fun createNewChain(@Header("session") session: String, @Body chain: Chain): Call<SecureBaseResponse<ChainCreatedContent>>

    @POST("chain/open")
    @FormUrlEncoded
    fun openChain(@Header("session") session: String, @Field("chain") chain: String): Call<SecureBaseResponse<ChainContent>>

    @DELETE("chain/close")
    fun closeChain(@Header("session") session: String, @Query("chain") chain: String): Call<SecureBaseResponse<ChainContent>>

    @GET("chain/info")
    fun getChains(@Header("session") session: String): Call<SecureBaseResponse<ChainFullContent>>

    @PUT("transaction/new")
    fun createNewTransaction(@Header("session") session: String, @Body transaction: Transaction): Call<SecureBaseResponse<TransactionCreatedContent>>

    @GET("transaction/info")
    fun getTransactionsByChain(@Header("session") session: String, @Query("chain") chain: String): Call<SecureBaseResponse<TransactionInfosContent>>

    @GET("transaction/info")
    fun getTransactionsBySender(@Header("session") session: String, @Query("sender") sender: String): Call<SecureBaseResponse<TransactionInfosContent>>

    @GET("transaction/info")
    fun getTransactionsByReceiver(@Header("session") session: String, @Query("receiver") chain: String): Call<SecureBaseResponse<TransactionInfosContent>>
}