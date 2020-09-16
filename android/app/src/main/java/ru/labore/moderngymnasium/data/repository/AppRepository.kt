package ru.labore.moderngymnasium.data.repository

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.Dispatcher
import org.threeten.bp.ZonedDateTime
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.daos.AnnouncementEntityDao
import ru.labore.moderngymnasium.data.db.daos.ClassEntityDao
import ru.labore.moderngymnasium.data.db.daos.RoleEntityDao
import ru.labore.moderngymnasium.data.db.daos.UserEntityDao
import ru.labore.moderngymnasium.data.db.entities.*
import ru.labore.moderngymnasium.data.network.AppNetwork
import ru.labore.moderngymnasium.data.network.SignIn
import ru.labore.moderngymnasium.data.sharedpreferences.entities.User

class AppRepository(
    private val context: Context,
    private val announcementEntityDao: AnnouncementEntityDao,
    private val userEntityDao: UserEntityDao,
    private val roleEntityDao: RoleEntityDao,
    private val classEntityDao: ClassEntityDao,
    private val appNetwork: AppNetwork
) {
    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences(
        context.getString(R.string.utility_shared_preference_file_key),
        Context.MODE_PRIVATE
    )

    var user: User? = null

    init {
        val userString = sharedPreferences.getString("user", null)

        if (userString != null) {
            user = gson.fromJson(userString, User::class.java)
        }

        appNetwork.fetchedAnnouncementEntities.observeForever {
            persistFetchedAnnouncements(it)
        }

        appNetwork.fetchedUserEntity.observeForever {
            persistFetchedUser(it)
        }

        appNetwork.fetchedRoleEntity.observeForever {
            persistFetchedRole(it)
        }

        appNetwork.fetchedClassEntity.observeForever {
            persistFetchedClass(it)
        }
    }

    suspend fun signIn(username: String, password: String) {
        user = SignIn(context, appNetwork, username, password)
        val editor = sharedPreferences.edit()

        editor.putString("user", gson.toJson(user))
        editor.apply()
    }

    suspend fun getAnnouncements(offset: Int = 0, limit: Int = 25): Array<AnnouncementEntity> {
        if (user == null) {
            return emptyArray()
        }

        val announcement =
            announcementEntityDao
                .getAnnouncementAtOffset(offset)
        val now = ZonedDateTime.now()
        val tenMinutesBefore = now.minusMinutes(10)

        if (
            announcement?.updatedAt?.isAfter(now) != false ||
            announcement.updatedAt!!.isBefore(tenMinutesBefore)
        ) {
            appNetwork.fetchAnnouncements(
                userEntityDao,
                roleEntityDao,
                classEntityDao,
                user!!.jwt,
                offset,
                limit
            )
        }

        val announcements = announcementEntityDao
            .getAnnouncements(offset, limit)

        List(announcements.size) {
            GlobalScope.launch {

                announcements[it].author = userEntityDao
                    .getUser(announcements[it].authorId)

                if (announcements[it].author != null) {
                    if (announcements[it].author!!.classId != null) {
                        announcements[it].authorClass =
                            classEntityDao.getClass(announcements[it].author!!.classId!!)
                    }

                    if (announcements[it].author!!.roleId != null) {
                        announcements[it].authorRole =
                            roleEntityDao.getRole(announcements[it].author!!.roleId!!)
                    }
                }
            }
        }.joinAll()

        return announcements
    }

    private fun persistFetchedAnnouncements(
        fetchedAnnouncements: Array<AnnouncementEntity>
    ) {
        val now = ZonedDateTime.now()

        fetchedAnnouncements.forEach {
            it.updatedAt = now
        }

        GlobalScope.launch(Dispatchers.IO) {
            announcementEntityDao.upsert(fetchedAnnouncements)
        }
    }

    private fun persistFetchedUser(fetchedUser: UserEntity) {
        fetchedUser.updatedAt = ZonedDateTime.now()

        GlobalScope.launch(Dispatchers.IO) {
            userEntityDao.upsert(fetchedUser)
        }
    }

    private fun persistFetchedRole(fetchedRole: RoleEntity) {
        fetchedRole.updatedAt = ZonedDateTime.now()

        GlobalScope.launch(Dispatchers.IO) {
            roleEntityDao.upsert(fetchedRole)
        }
    }

    private fun persistFetchedClass(fetchedClass: ClassEntity) {
        fetchedClass.updatedAt = ZonedDateTime.now()

        GlobalScope.launch(Dispatchers.IO) {
            classEntityDao.upsert(fetchedClass)
        }
    }
}