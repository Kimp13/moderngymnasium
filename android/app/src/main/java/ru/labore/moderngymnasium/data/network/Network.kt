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

class AppNetwork(context: Context) : Interceptor {
    private val appContext = context.applicationContext
    val fetchedAnnouncementEntities = MutableLiveData<Array<AnnouncementEntity>>()
    val fetchedUserEntity = MutableLiveData<UserEntity>()
    val fetchedRoleEntity = MutableLiveData<RoleEntity>()
    val fetchedClassEntity = MutableLiveData<ClassEntity>()

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

    suspend fun fetchClass(
        id: Int
    ) = FetchClass(
        appContext,
        this,
        id
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        if (isOnline()) {
            val response = chain.proceed(chain.request())

            if (response.code() in 400..499) {
                throw ClientErrorException(response.code())
            }

            return response
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