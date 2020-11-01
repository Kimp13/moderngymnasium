package ru.labore.moderngymnasium.data.network

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.Response
import org.threeten.bp.ZonedDateTime
import ru.labore.moderngymnasium.data.db.daos.ClassEntityDao
import ru.labore.moderngymnasium.data.db.daos.RoleEntityDao
import ru.labore.moderngymnasium.data.db.daos.UserEntityDao
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.data.db.entities.ClassEntity
import ru.labore.moderngymnasium.data.db.entities.RoleEntity
import ru.labore.moderngymnasium.data.db.entities.UserEntity
import java.net.ConnectException
import java.net.SocketTimeoutException

class AppNetwork(context: Context) : Interceptor {
    private val appContext = context.applicationContext
    val fetchedAnnouncementEntities = MutableLiveData<Array<AnnouncementEntity>>()
    val fetchedUserEntity = MutableLiveData<UserEntity>()
    val fetchedRoleEntity = MutableLiveData<RoleEntity>()
    val fetchedClassEntity = MutableLiveData<ClassEntity>()

    suspend fun pushToken(
        jwt: String,
        token: String
    ) = PushToken(
        appContext,
        this,
        jwt,
        token
    )

    suspend fun fetchAnnouncement(
        jwt: String,
        id: Int,
        gson: Gson
    ) = FetchAnnouncement(
        appContext,
        this,
        gson,
        jwt,
        id
    )

    suspend fun fetchAnnouncements(
        jwt: String,
        offset: Int,
        limit: Int,
        gson: Gson
    ) = FetchAnnouncements(
        appContext,
        this,
        gson,
        jwt,
        offset,
        limit
    )

    suspend fun countAnnouncements(
        jwt: String
    ) = CountAnnouncements(
        appContext,
        this,
        jwt
    )

    suspend fun fetchUser(
        id: Int
    ) = FetchUser(
        appContext,
        this,
        id
    )

    suspend fun fetchRole(
        id: Int
    ) = FetchRole(
        appContext,
        this,
        id
    )

    suspend fun fetchAllRoles() = FetchAllRoles(appContext, this)

    suspend fun fetchClass(
        id: Int
    ) = FetchClass(
        appContext,
        this,
        id
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        if (isOnline()) {
            try {
                val response = chain.proceed(chain.request())

                if (response.code() in 400..499) {
                    throw ClientErrorException(response.code())
                } else if (response.code() in 500..599) {
                    throw ConnectException()
                }

                return response
            } catch(e: SocketTimeoutException) {
                throw ConnectException()
            }
        } else {
            throw ClientConnectionException()
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo

        return (networkInfo != null && networkInfo.isConnected)
    }
}