package ru.labore.moderngymnasium.data.repository

import android.content.Context
import com.google.gson.*
import kotlinx.coroutines.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.daos.AnnouncementEntityDao
import ru.labore.moderngymnasium.data.db.daos.ClassEntityDao
import ru.labore.moderngymnasium.data.db.daos.RoleEntityDao
import ru.labore.moderngymnasium.data.db.daos.UserEntityDao
import ru.labore.moderngymnasium.data.db.entities.*
import ru.labore.moderngymnasium.data.network.*
import ru.labore.moderngymnasium.data.sharedpreferences.entities.User
import java.lang.reflect.Type

class JsonSerializerImpl : JsonSerializer<ZonedDateTime> {
    override fun serialize(
        src: ZonedDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return if (src == null) {
            JsonPrimitive("")
        } else {
            JsonPrimitive(src.toString())
        }
    }
}

class JsonDeserializerImpl : JsonDeserializer<ZonedDateTime> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ZonedDateTime {
        return if (json == null) {
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.UTC)
        } else {
            ZonedDateTime.parse(json.asString)
        }
    }
}

class AppRepository(
    private val context: Context,
    private val announcementEntityDao: AnnouncementEntityDao,
    private val userEntityDao: UserEntityDao,
    private val roleEntityDao: RoleEntityDao,
    private val classEntityDao: ClassEntityDao,
    private val appNetwork: AppNetwork
) {
    private val gson = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, JsonSerializerImpl())
        .registerTypeAdapter(ZonedDateTime::class.java, JsonDeserializerImpl())
        .create()
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

    suspend fun createAnnouncement(
        text: String
    ) {
        if (user?.jwt != null) {
            CreateAnnouncement(
                context,
                appNetwork,
                user!!.jwt,
                text
            )
        }
    }

    suspend fun getAnnouncements(offset: Int = 0, limit: Int = 25): Array<AnnouncementEntity> {
        var announcements: Array<AnnouncementEntity>

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
            announcements = appNetwork.fetchAnnouncements(
                user!!.jwt,
                offset,
                limit,
                gson
            )
            val oneDayBefore = ZonedDateTime.now().minusDays(1)

            List(announcements.size) {
                GlobalScope.launch {
                    announcements[it].author = userEntityDao
                        .getUser(announcements[it].authorId)

                    if (
                        announcements[it].author
                            ?.updatedAt
                            ?.isBefore(oneDayBefore) != false
                    ) {
                        announcements[it].author = appNetwork.fetchUser(
                            announcements[it].authorId
                        )

                        if (announcements[it].author != null) {
                            val weekBefore = ZonedDateTime.now().minusWeeks(1)

                            if (announcements[it].author!!.roleId != null) {
                                announcements[it].authorRole = roleEntityDao.getRole(
                                    announcements[it].author!!.roleId!!
                                )

                                if (
                                    announcements[it].authorRole
                                        ?.updatedAt
                                        ?.isBefore(weekBefore) != false
                                ) {
                                    announcements[it].authorRole = appNetwork.fetchRole(
                                        announcements[it].author!!.roleId!!
                                    )

                                    if (announcements[it].authorRole != null) {
                                        persistFetchedRole(
                                            announcements[it].authorRole!!
                                        )
                                    }
                                }
                            }

                            if (announcements[it].author!!.classId != null) {
                                announcements[it].authorClass = classEntityDao.getClass(
                                    announcements[it].author!!.classId!!
                                )

                                if (
                                    announcements[it].authorClass
                                        ?.updatedAt
                                        ?.isBefore(weekBefore) != false
                                ) {
                                    announcements[it].authorClass = appNetwork.fetchClass(
                                        announcements[it].author!!.classId!!
                                    )

                                    if (announcements[it].authorClass != null) {
                                        persistFetchedClass(
                                            announcements[it].authorClass!!
                                        )
                                    }
                                }
                            }

                            persistFetchedUser(announcements[it].author!!)
                        }
                    }
                }
            }.joinAll()

            persistFetchedAnnouncements(announcements)
        } else {
            announcements = announcementEntityDao
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
        }

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