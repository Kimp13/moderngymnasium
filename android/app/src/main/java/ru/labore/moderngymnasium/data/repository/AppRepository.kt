package ru.labore.moderngymnasium.data.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
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

data class UpdatedAnnouncementInfo(
    val users: HashMap<Int, Deferred<UserEntity?>>,
    val classes: HashMap<Int, Deferred<ClassEntity?>>,
    val roles: HashMap<Int, Deferred<RoleEntity?>>
)

data class AnnouncementsWithCount(
    val overallCount: Int,
    var currentCount: Int,
    val data: Array<AnnouncementEntity>
)

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
    private var token = ""

    val inboxAnnouncement: MutableLiveData<AnnouncementEntity> =
        MutableLiveData()

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

    fun pushToken(tkn: String) {
        println("Got new token as $tkn")
        if (user?.jwt != null) {
            println("Launching it!")
            GlobalScope.launch {
                appNetwork.pushToken(user!!.jwt, token)
            }
        } else {
            token = tkn
        }
    }

    suspend fun signIn(username: String, password: String) {
        user = SignIn(context, appNetwork, username, password)
        val editor = sharedPreferences.edit()

        if (token.isNotEmpty()) {
            pushToken(token)
        }

        editor.putString("user", gson.toJson(user))
        editor.apply()
    }

    suspend fun createAnnouncement(
        text: String,
        recipients: Array<Int>
    ) {
        if (user?.jwt != null) {
            CreateAnnouncement(
                context,
                appNetwork,
                user!!.jwt,
                text,
                recipients
            )
        }
    }

    fun fetchDeferredUser(id: Int) = GlobalScope.async {
        appNetwork.fetchUser(id)
    }

    fun fetchDeferredRole(id: Int) = GlobalScope.async {
        appNetwork.fetchRole(id)
    }

    fun fetchDeferredClass(id: Int) = GlobalScope.async {
        appNetwork.fetchClass(id)
    }

    suspend fun populateAnnouncementEntity(
        entity: AnnouncementEntity,
        updated: UpdatedAnnouncementInfo = UpdatedAnnouncementInfo(
            HashMap(),
            HashMap(),
            HashMap()
        ),
        forceFetch: Boolean = false
    ) {
        val oneDayBefore = ZonedDateTime.now().minusDays(1)
        if (!forceFetch) {
            entity.author = userEntityDao
                .getUser(entity.authorId)
        }

        if (
            forceFetch ||
            entity.author
                ?.updatedAt
                ?.isBefore(oneDayBefore) != false
        ) {
            if (!updated.users.containsKey(entity.authorId)) {
                updated.users[entity.authorId] = fetchDeferredUser(
                    entity.authorId
                )
            }

            entity.author =
                updated.users[entity.authorId]?.await()

            persistFetchedUser(entity.author!!)
        }

        if (entity.author != null) {
            val weekBefore = ZonedDateTime.now().minusWeeks(1)

            if (entity.author!!.roleId != null) {
                if (!forceFetch) {
                    entity.authorRole = roleEntityDao.getRole(
                        entity.author!!.roleId!!
                    )
                }

                if (
                    forceFetch ||
                    entity.authorRole
                        ?.updatedAt
                        ?.isBefore(weekBefore) != false
                ) {
                    if (
                        !updated.roles.containsKey(entity.author!!.roleId)
                    ) {
                        updated.roles[entity.author!!.roleId!!] =
                            fetchDeferredRole(entity.author!!.roleId!!)
                    }

                    entity.authorRole =
                        updated.roles[entity.author!!.roleId!!]?.await()

                    if (entity.authorRole != null) {
                        persistFetchedRole(
                            entity.authorRole!!
                        )
                    }
                }
            }

            if (entity.author!!.classId != null) {
                if (!forceFetch) {
                    entity.authorClass = classEntityDao.getClass(
                        entity.author!!.classId!!
                    )
                }

                if (
                    forceFetch ||
                    entity.authorClass
                        ?.updatedAt
                        ?.isBefore(weekBefore) != false
                ) {
                    if (!updated.classes.containsKey(entity.author!!.classId)) {
                        updated.classes[entity.author!!.classId!!] =
                            fetchDeferredClass(entity.author!!.classId!!)
                    }

                    entity.authorClass =
                        updated.classes[entity.author!!.classId!!]?.await()

                    if (entity.authorClass != null) {
                        persistFetchedClass(
                            entity.authorClass!!
                        )
                    }
                }
            }
        }
    }

    fun pushNewAnnouncement(map: Map<String, String>) {
        if (
            map["id"] != null &&
            map["text"] != null &&
            map["created_at"] != null &&
            map["author_id"] != null
        ) {
            GlobalScope.launch {
                val entity =  AnnouncementEntity(
                    (map["id"] ?: error("")).toInt(),
                    ZonedDateTime.parse(map["created_at"]),
                    (map["author_id"] ?: error("")).toInt(),
                    map["text"] ?: error("")
                )

                populateAnnouncementEntity(entity)

                inboxAnnouncement.postValue(entity)
            }
        }
    }

    suspend fun getAnnouncements(
        offset: Int = 0,
        limit: Int = 25,
        forceFetch: Boolean = false
    ): AnnouncementsWithCount {
        val announcements: AnnouncementsWithCount

        if (user == null) {
            return AnnouncementsWithCount(0, 0, emptyArray())
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
            announcements = AnnouncementsWithCount(
                appNetwork.countAnnouncements(user!!.jwt),
                0,
                appNetwork.fetchAnnouncements(
                    user!!.jwt,
                    offset,
                    limit,
                    gson
                )
            )

            announcements.currentCount = announcements.data.size

            persistFetchedAnnouncements(announcements.data)
        } else {
            announcements = AnnouncementsWithCount(
                announcementEntityDao.countAnnouncements(),
                0,
                announcementEntityDao.getAnnouncements(offset, limit)
            )

            announcements.currentCount = announcements.data.size
        }

        val updated = UpdatedAnnouncementInfo(
            HashMap(),
            HashMap(),
            HashMap()
        )

        List(announcements.data.size) {
            GlobalScope.launch {
                populateAnnouncementEntity(announcements.data[it], updated, forceFetch)
            }
        }.joinAll()

        return announcements
    }

    suspend fun getUserRoles(): Array<RoleEntity?> = if (
        user?.data?.permissions?.announcement?.create != null
    ) {
        getRoles(user!!.data.permissions!!.announcement!!.create!!)
    } else {
        emptyArray()
    }

    private suspend fun getRoles(rolesIds: Array<Int>): Array<RoleEntity?> {
        val result: Array<RoleEntity?> = arrayOfNulls(rolesIds.size)

        List(rolesIds.size) {
            GlobalScope.launch {
                var role = roleEntityDao.getRole(rolesIds[it])

                if (role == null) {
                    role = appNetwork.fetchRole(rolesIds[it])

                    if (role != null) {
                        persistFetchedRole(role)
                    }
                }

                result[it] = role
            }
        }.joinAll()

        return result
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