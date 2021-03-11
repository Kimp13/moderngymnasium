package ru.labore.eventeger.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.Response
import org.threeten.bp.ZonedDateTime
import ru.labore.eventeger.data.db.entities.AnnouncementEntity
import ru.labore.eventeger.data.db.entities.ClassEntity
import ru.labore.eventeger.data.db.entities.RoleEntity
import ru.labore.eventeger.data.db.entities.UserEntity
import ru.labore.eventeger.data.network.exceptions.ClientConnectionException
import ru.labore.eventeger.data.network.exceptions.ClientErrorException
import java.net.ConnectException
import java.net.SocketTimeoutException

class AppNetwork(context: Context, gson: Gson) : Interceptor {
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

    suspend fun fetchComment(
        jwt: String,
        id: Int
    ) = utility.fetchComment(jwt, id)

    suspend fun fetchComments(
        jwt: String,
        announcementId: Int,
        offset: Int,
        replyTo: Int?
    ) = utility.fetchComments(jwt, announcementId, offset, replyTo)

    suspend fun fetchAnnouncement(
        jwt: String,
        id: Int
    ) = utility.fetchAnnouncement(jwt, id)

    suspend fun fetchAnnouncements(
        jwt: String,
        offset: Int
    ) = utility.fetchAnnouncements(jwt, offset)

    suspend fun createComment(
        jwt: String,
        announcementId: Int,
        text: String,
        hidden: Boolean,
        replyTo: Int? = null
    ) = utility.createComment(jwt, announcementId, text, hidden, replyTo)

    suspend fun createAnnouncement(
        jwt: String,
        text: String,
        recipients: HashMap<Int, HashSet<Int>>
    ) = utility.createAnnouncement(jwt, text, recipients)

    suspend fun createAnnouncement(
        jwt: String,
        text: String,
        recipients: HashMap<Int, HashSet<Int>>,
        beginsAt: ZonedDateTime?,
        endsAt: ZonedDateTime?
    ) = utility.createAnnouncement(jwt, text, recipients, beginsAt, endsAt)

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
            } catch (e: SocketTimeoutException) {
                throw ConnectException()
            }
        } else {
            throw ClientConnectionException()
        }
    }

    fun isOnline(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager? ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return (
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ||
                            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        } else {
            @Suppress("DEPRECATION")
            return connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }
}