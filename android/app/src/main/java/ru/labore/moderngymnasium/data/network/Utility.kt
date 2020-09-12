package ru.labore.moderngymnasium.data.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.user.User

data class UserCredentials(val username: String, val password: String)

interface UserSignIn {
    @POST("users/signin")
    suspend fun signIn(@Body body: UserCredentials): User?

    companion object {
        operator fun invoke(
            context: Context,
            connectInterceptor: Interceptor
        ): UserSignIn {
            val okHttpClient = OkHttpClient
                .Builder()
                .addInterceptor(connectInterceptor)
                .build()

            return Retrofit
                .Builder()
                .client(okHttpClient)
                .baseUrl(
                    context
                        .resources
                        .getString(R.string.api_url)
                )
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UserSignIn::class.java)
        }
    }
}