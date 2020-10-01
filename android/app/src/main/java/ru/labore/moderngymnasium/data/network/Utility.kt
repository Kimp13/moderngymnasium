package ru.labore.moderngymnasium.data.network

import android.content.Context
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.data.db.entities.ClassEntity
import ru.labore.moderngymnasium.data.db.entities.RoleEntity
import ru.labore.moderngymnasium.data.db.entities.UserEntity
import ru.labore.moderngymnasium.data.sharedpreferences.entities.User

data class UserCredentials(val username: String, val password: String)

data class AnnouncementTextAndRecipients(
    val text: String,
    val recipients: HashMap<Int, MutableList<Int>>
)

data class TokenPayload(val token: String)

data class CountResponse(val count: Int)

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

interface CreateAnnouncement {
    @POST("announcements/create")
    suspend fun createAnnouncement(
        @Header("Authentication") jwt: String,
        @Body body: AnnouncementTextAndRecipients
    )

    companion object {
        suspend operator fun invoke(
            context: Context,
            requestInterceptor: Interceptor,
            jwt: String,
            text: String,
            recipients: HashMap<Int, MutableList<Int>>
        ) {
            val okHttpClient = OkHttpClient
                .Builder()
                .addInterceptor(requestInterceptor)
                .build()

            Retrofit
                .Builder()
                .client(okHttpClient)
                .baseUrl(
                    context
                        .resources
                        .getString(R.string.api_url)
                )
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CreateAnnouncement::class.java)
                .createAnnouncement(
                    jwt,
                    AnnouncementTextAndRecipients(text, recipients)
                )
        }
    }
}

interface PushToken {
    @POST("tokens/add")
    suspend fun pushToken(
        @Header("Authentication") jwt: String,
        @Body body: TokenPayload
    )

    companion object {
        suspend operator fun invoke(
            context: Context,
            requestInterceptor: Interceptor,
            jwt: String,
            token: String
        ) {
            val okHttpClient = OkHttpClient
                .Builder()
                .addInterceptor(requestInterceptor)
                .build()

            Retrofit
                .Builder()
                .client(okHttpClient)
                .baseUrl(
                    context
                        .resources
                        .getString(R.string.api_url)
                )
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PushToken::class.java)
                .pushToken(
                    jwt,
                    TokenPayload(token)
                )
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
            gson: Gson,
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
                .addConverterFactory(GsonConverterFactory.create(gson))
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

interface CountAnnouncements {
    @GET("announcements/countMine")
    suspend fun count(
        @Header("Authentication") jwt: String
    ): CountResponse

    companion object {
        suspend operator fun invoke(
            context: Context,
            requestInterceptor: Interceptor,
            jwt: String
        ): Int {
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
                .create(CountAnnouncements::class.java)
                .count(
                    jwt
                ).count
        }
    }
}

interface FetchUser {
    @GET("users")
    suspend fun fetch(@Query("id") id: Int): UserEntity?

    companion object {
        suspend operator fun invoke(
            context: Context,
            requestInterceptor: Interceptor,
            id: Int
        ): UserEntity? {
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

interface FetchRole {
    @GET("roles")
    suspend fun fetch(@Query("id") id: Int): RoleEntity?

    companion object {
        suspend operator fun invoke(
            context: Context,
            requestInterceptor: Interceptor,
            id: Int
        ): RoleEntity? {
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
                .create(FetchRole::class.java)
                .fetch(id)
        }
    }
}

interface FetchClass {
    @GET("class")
    suspend fun fetch(@Query("id") id: Int): ClassEntity?

    companion object {
        suspend operator fun invoke(
            context: Context,
            requestInterceptor: Interceptor,
            id: Int
        ): ClassEntity? {
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
                .create(FetchClass::class.java)
                .fetch(id)
        }
    }
}