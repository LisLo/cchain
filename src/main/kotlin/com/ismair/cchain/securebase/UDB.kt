package com.ismair.cchain.securebase

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*
import java.io.Serializable
import java.util.*

interface UDB {
    companion object {
        const val OPTIMISTIC = 'O'
        const val PESSIMISTIC = 'P'
    }

    data class User(
            val user: String,
            val password: String,
            val device: String,
            val email: String,
            @SerializedName("modus") val mode: Char = OPTIMISTIC
    )

    data class Credentials(
            val user: String,
            val password: String,
            val device: String
    )

    data class UserOptionalInfo(
            val name: String,
            val email: String,
            val phone: String,
            val city: String,
            val company: String,
            val social: String
    )

    data class Key(
            val key: String,
            @SerializedName("pubkey") val publicKey: String
    )

    data class RegistrationContent(
            val user: String,
            val password: String,
            val uid: Int,
            val modus: Char,
            val device: String,
            val did: Int,
            val status: String
    )

    data class SessionCreatedContent(
            val user: String,
            val password: String,
            val uid: Int,
            val device: String,
            val did: Int,
            val session: String,
            val timeout: Int,
            val started: Date
    )

    data class SessionDestroyedContent(
            val user: String,
            val session: String,
            val started: Date
    )

    data class UserContent (
            val user: String,
            val uid: Int,
            val name: String?,
            val email: String?,
            val phone: String?,
            val city: String?,
            val company: String?,
            val social: String?
    )

    data class UserWithDeviceContent (
            val user: String,
            val uid: Int,
            val device: String,
            val did: Int,
            val name: String?,
            val email: String?,
            val phone: String?,
            val city: String?,
            val company: String?,
            val social: String?
    ) : Serializable

    data class KeyContent(
            val key: String,
            val kid: Int
    )

    data class KeyFullContent(
            val user: String,
            val uid: Int,
            val device: String,
            val did: Int,
            val status: Char,
            val key: String,
            val kid: Int,
            @SerializedName("pubkey") val publicKey: String
    )

    @PUT("users/register")
    fun register(@Body user: User): Call<SecureBaseResponse<RegistrationContent>>

    @PUT("users/login")
    fun login(@Body credentials: Credentials): Call<SecureBaseResponse<SessionCreatedContent>>

    @DELETE("users/logout")
    fun logout(@Header("session") session: String): Call<SecureBaseResponse<SessionDestroyedContent>>

    @POST("users/modify")
    fun modifyUser(@Header("session") session: String, @Body userOptionalInfo: UserOptionalInfo): Call<SecureBaseResponse<UserContent>>

    @GET("users/retrieve")
    fun retrieveUsers(
            @Header("session") session: String,
            @Query("user") user: String,
            @Query("name") name: String,
            @Query("email") email: String,
            @Query("phone") phone: String,
            @Query("city") city: String,
            @Query("company") company: String,
            @Query("social") social: String
    ): Call<SecureBaseResponse<UserWithDeviceContent>>

    @PUT("keys/attach")
    fun attachKey(@Header("session") session: String, @Body key: Key): Call<SecureBaseResponse<KeyContent>>

    @GET("keys/fetch")
    fun fetchKeys(
            @Header("session") session: String,
            @Query("user") user: String?,
            @Query("device") device: String?,
            @Query("key") key: String?,
            @Query("pubkey") publicKey: String?
    ): Call<SecureBaseResponse<KeyFullContent>>
}