package ru.labore.moderngymnasium.data.network

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.MutableLiveData
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
        userEntityDao: UserEntityDao,
        roleEntityDao: RoleEntityDao,
        classEntityDao: ClassEntityDao,
        jwt: String,
        offset: Int,
        limit: Int
    ) {
        val announcements = FetchAnnouncements(
            appContext,
            this,
            jwt,
            offset,
            limit
        )
        val oneDayBefore = ZonedDateTime.now().minusDays(1)
        List(announcements.size) {
            GlobalScope.launch {
                var user = userEntityDao
                    .getUserLastUpdatedTime(announcements[it].authorId)
                if (
                    user?.updatedAt?.isBefore(oneDayBefore) != false
                ) {
                    user = FetchUser(
                        appContext,
                        this@AppNetwork,
                        announcements[it].authorId
                    )

                    if (user != null) {
                        val weekBefore = ZonedDateTime.now().minusWeeks(1)

                        if (user.roleId != null) {
                            var role = roleEntityDao.getRole(user.roleId!!)

                            if (
                                role?.updatedAt?.isBefore(weekBefore) != false
                            ) {
                                role = FetchRole(
                                    appContext,
                                    this@AppNetwork,
                                    user.roleId!!
                                )

                                if (role != null) {
                                    fetchedRoleEntity.postValue(role)
                                }
                            }
                        }

                        if (user.classId != null) {
                            var classEntity = classEntityDao.getClass(user.classId!!)

                            if (
                                classEntity?.updatedAt?.isBefore(weekBefore) != false
                            ) {
                                classEntity = FetchClass(
                                    appContext,
                                    this@AppNetwork,
                                    user.classId!!
                                )

                                if (classEntity != null) {
                                    fetchedClassEntity.postValue(classEntity)
                                }
                            }
                        }

                        fetchedUserEntity.postValue(user)
                    }
                }
            }
        }.joinAll()

        fetchedAnnouncementEntities.postValue(announcements)
    }

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