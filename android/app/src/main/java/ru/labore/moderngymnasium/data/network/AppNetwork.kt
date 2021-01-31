package ru.labore.moderngymnasium.data.network

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.Response
import org.threeten.bp.ZonedDateTime
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.data.db.entities.ClassEntity
import ru.labore.moderngymnasium.data.db.entities.RoleEntity
import ru.labore.moderngymnasium.data.db.entities.UserEntity
import java.net.ConnectException
import java.net.SocketTimeoutException

class AppNetwork(context: Context, private val gson: Gson) : Interceptor {
    private val appContext = context.applicationContext
    private val utility = Utility(appContext, this, gson)
    val fetchedAnnouncementEntities = MutableLiveData<Array<AnnouncementEntity>>()
    val fetchedUserEntity = MutableLiveData<UserEntity>()
    val fetchedRoleEntity = MutableLiveData<RoleEntity>()
    val fetchedClassEntity = MutableLiveData<ClassEntity>()

    suspend fun pushToken(
        jwt: String,
        token: String
    ) = utility.pushToken(jwt, token)

    suspend fun fetchMe(
        jwt: String
    ) = utility.fetchMe(jwt)

    suspend fun fetchAnnounceMap(
        jwt: String
    ) = utility.fetchAnnouncementMap(jwt)

    suspend fun fetchAnnouncement(
        jwt: String,
        id: Int
    ) = utility.fetchAnnouncement(jwt, id)

    suspend fun fetchAnnouncements(
        jwt: String,
        offset: ZonedDateTime
    ) = utility.fetchAnnouncements(jwt, offset)

    suspend fun countAnnouncements(
        jwt: String
    ) = utility.countAnnouncements(jwt)

    suspend fun createAnnouncement(
        jwt: String,
        text: String,
        recipients: HashMap<Int, HashSet<Int>>
    ) = utility.createAnnouncement(jwt, text, recipients)

    suspend fun signIn(
        username: String,
        password: String
    ) = utility.signIn(username, password)

    suspend fun fetchUser(
        id: Int
    ) = utility.fetchUser(id)

    suspend fun fetchUsers(
        ids: Array<Int>
    ) = utility.fetchUsers(ids)

    suspend fun fetchRole(
        id: Int
    ) = utility.fetchRole(id)

    suspend fun fetchRoles(
        ids: Array<Int>
    ) = utility.fetchRoles(ids)

    suspend fun fetchAllRoles(jwt: String) = utility.fetchAllRoles(jwt)

    suspend fun fetchClass(
        id: Int
    ) = utility.fetchClass(id)

    suspend fun fetchClasses(
        ids: Array<Int>
    ) = utility.fetchClasses(ids)

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