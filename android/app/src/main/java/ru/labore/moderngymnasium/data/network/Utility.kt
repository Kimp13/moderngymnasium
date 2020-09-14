package ru.labore.moderngymnasium.data.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.data.db.entities.UserEntity
import ru.labore.moderngymnasium.data.sharedpreferences.entities.User

data class UserCredentials(val username: String, val password: String)

interface SignIn {
    @POST("users/signin")
    suspend fun signIn(@Body body: UserCredentials): User?

    companion object {
        suspend operator fun invoke(
            context: Context,
            requestInterceptor: Interceptor,
            username: String,
            password: String
        ): User? {
            val okHttpClient = OkHttpClient
                .Builder()
                .addInterceptor(requestInterceptor)
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
                .create(SignIn::class.java)
                .signIn(UserCredentials(username, password))
        }
    }
}

interface FetchAnnouncements {
    @GET("announcements/getMine")
    suspend fun fetch(
        @Header("Authentication") jwt: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Array<AnnouncementEntity>

    companion object {
        suspend operator fun invoke(
            context: Context,
            requestInterceptor: Interceptor,
            jwt: String,
            offset: Int,
            limit: Int
        ): Array<AnnouncementEntity> {
            val okHttpClient = OkHttpClient
                .Builder()
                .addInterceptor(requestInterceptor)
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
                .create(FetchAnnouncements::class.java)
                .fetch(
                    jwt,
                    offset,
                    limit
                )
        }
    }
}

interface FetchUser {
    @GET("users")
    suspend fun fetch(@Query("id") id: Int): UserEntity

    companion object {
        suspend operator fun invoke(
            context: Context,
            requestInterceptor: Interceptor,
            id: Int
        ): UserEntity {
            val okHttpClient = OkHttpClient
                .Builder()
                .addInterceptor(requestInterceptor)
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
                .create(FetchUser::class.java)
                .fetch(id)
        }
    }
}