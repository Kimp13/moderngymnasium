package ru.labore.moderngymnasium.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.threeten.bp.ZonedDateTime
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.daos.AnnouncementEntityDao
import ru.labore.moderngymnasium.data.db.daos.ClassEntityDao
import ru.labore.moderngymnasium.data.db.daos.RoleEntityDao
import ru.labore.moderngymnasium.data.db.daos.UserEntityDao
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.data.db.entities.ClassEntity
import ru.labore.moderngymnasium.data.db.entities.RoleEntity
import ru.labore.moderngymnasium.data.db.entities.UserEntity
import ru.labore.moderngymnasium.data.network.AppNetwork
import ru.labore.moderngymnasium.data.sharedpreferences.entities.AnnounceMap
import ru.labore.moderngymnasium.data.sharedpreferences.entities.User
import ru.labore.moderngymnasium.utils.announcementEntityToCaption

class AppRepository(
    private val context: Context,
    private val announcementEntityDao: AnnouncementEntityDao,
    private val userEntityDao: UserEntityDao,
    private val roleEntityDao: RoleEntityDao,
    private val classEntityDao: ClassEntityDao,
    private val appNetwork: AppNetwork,
    private val gson: Gson
) {
    companion object {
        const val DEFAULT_LIMIT = 25

        const val HTTP_RESPONSE_CODE_UNAUTHORIZED = 401

        enum class UpdateParameters {
            UPDATE, DONT_UPDATE, DETERMINE
        }

        data class DeferredAnnouncementInfo(
            val users: HashMap<Int, Deferred<UserEntity?>>,
            val classes: HashMap<Int, Deferred<ClassEntity?>>,
            val roles: HashMap<Int, Deferred<RoleEntity?>>
        )
    }

    val inboxAnnouncement: MutableLiveData<AnnouncementEntity> =
        MutableLiveData()

    var isAppForeground: Boolean = true
    var user: User? = null
    var announceMap = AnnounceMap()
    var unreadAnnouncements: MutableList<AnnouncementEntity> = mutableListOf()
    var unreadAnnouncementsPushListener: ((AnnouncementEntity) -> Unit)? = null

    private val sharedPreferences = context.getSharedPreferences(
        context.getString(R.string.utility_shared_preference_file_key),
        Context.MODE_PRIVATE
    )
    private val notificationBuilder = NotificationCompat.Builder(
        context,
        context.getString(R.string.default_notification_channel_id)
    )

    init {
        val userString = sharedPreferences.getString("user", null)

        if (userString != null) {
            user = gson.fromJson(userString, User::class.java)
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_baseline_announcement)

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

    suspend fun onMainActivityCreated() {
        if (user != null) {
            val new = appNetwork.fetchMe(user!!.jwt)
            val editor = sharedPreferences.edit()

            if (new != null && user != null) {
                user = User(user!!.jwt, new.data)

                editor.putString("user", gson.toJson(user))
            }

            getMap(editor)
        }
    }

    private suspend fun getMap(
        editor: SharedPreferences.Editor = sharedPreferences.edit()
    ) {
        val mapString = sharedPreferences.getString("announce_map", null)

        announceMap = if (mapString == null) {
            AnnounceMap()
        } else {
            gson.fromJson(mapString, AnnounceMap::class.java)
        }

        val newMap = appNetwork.fetchAnnounceMap(user!!.jwt)

        announceMap = newMap
        editor.putString("announce_map", gson.toJson(newMap))

        editor.apply()
    }

    fun saveToken(token: String) {
        val editor = sharedPreferences.edit()

        pushToken(token)
        editor.putString("messaging_token", token)

        editor.apply()
    }

    private fun pushToken(token: String? = null) {
        if (user?.jwt != null) {
            val actualToken =
                token ?: sharedPreferences.getString("messaging_token", null)

            if (actualToken?.isNotEmpty() == true) {
                GlobalScope.launch {
                    appNetwork.pushToken(user!!.jwt, actualToken)
                }
            }
        }
    }

    suspend fun signIn(username: String, password: String) {
        user = appNetwork.signIn(username, password)
        val editor = sharedPreferences.edit()

        pushToken()
        editor.putString("user", gson.toJson(user))

        getMap(editor)
    }

    suspend fun createAnnouncement(
        text: String,
        recipients: HashMap<Int, HashSet<Int>>
    ) {
        if (user?.jwt != null) {
            appNetwork.createAnnouncement(
                user!!.jwt,
                text,
                recipients
            )
        }
    }

    private fun fetchDeferredUser(id: Int) = GlobalScope.async {
        appNetwork.fetchUser(id)
    }

    private fun fetchDeferredRole(id: Int) = GlobalScope.async {
        appNetwork.fetchRole(id)
    }

    private fun fetchDeferredClass(id: Int) = GlobalScope.async {
        appNetwork.fetchClass(id)
    }

    private suspend fun populateAnnouncements(
        entities: Array<AnnouncementEntity>,
        forceFetch: UpdateParameters = UpdateParameters.DETERMINE
    ) {
        val now = ZonedDateTime.now()
        val roles = HashMap<Int, RoleEntity>()
        val classes = HashMap<Int, ClassEntity>()
        val users = HashMap<Int, UserEntity>()
        val rolesToUpdate = HashSet<Int>()
        val classesToUpdate = HashSet<Int>()
        val usersToUpdate = HashSet<Int>()

        when (forceFetch) {
            UpdateParameters.UPDATE -> entities.forEach {
                usersToUpdate.add(it.authorId)
            }
            UpdateParameters.DETERMINE -> entities.forEach {
                it.author = userEntityDao.getUser(it.authorId)

                if (
                    it.author?.updatedAt == null ||
                    now <= it.author!!.updatedAt!!.plusWeeks(1)
                ) {
                    usersToUpdate.add(it.authorId)
                }
            }
            else -> entities.forEach {
                it.author = userEntityDao.getUser(it.authorId)
            }
        }

        if (forceFetch != UpdateParameters.DONT_UPDATE && usersToUpdate.isNotEmpty()) {
            val fetchedUsers = appNetwork.fetchUsers(usersToUpdate.toTypedArray())

            fetchedUsers.forEach {
                users[it.id] = it
            }

            entities.forEach {
                if (users.containsKey(it.authorId))
                    it.author = users[it.authorId]!!
            }
        }

        when (forceFetch) {
            UpdateParameters.UPDATE -> entities.forEach {
                if (it.author?.roleId != null)
                    rolesToUpdate.add(it.author!!.roleId!!)

                if (it.author?.classId != null)
                    classesToUpdate.add(it.author!!.classId!!)
            }
            UpdateParameters.DETERMINE -> entities.forEach {
                if (it.author?.roleId != null)
                    it.authorRole = roleEntityDao.getRole(it.author!!.roleId!!)

                if (it.author?.classId != null)
                    it.authorClass = classEntityDao.getClass(it.author!!.classId!!)

                if (
                    it.authorRole?.updatedAt == null ||
                            now < it.authorRole!!.updatedAt!!.plusWeeks(1)
                )
                    if (it.author?.roleId != null)
                        rolesToUpdate.add(it.author!!.roleId!!)

                if (
                    it.authorClass?.updatedAt == null ||
                    now < it.authorClass!!.updatedAt!!.plusWeeks(1)
                )
                    if (it.author?.classId != null)
                        classesToUpdate.add(it.author!!.classId!!)
            }
            else -> entities.forEach {
                if (it.author?.roleId != null)
                    it.authorRole = roleEntityDao.getRole(it.author!!.roleId!!)

                if (it.author?.classId != null)
                    it.authorClass = classEntityDao.getClass(it.author!!.classId!!)
            }
        }

        if (forceFetch != UpdateParameters.DONT_UPDATE) {
            listOf(GlobalScope.launch {
                if (rolesToUpdate.isNotEmpty()) {
                    val fetchedRoles =
                        appNetwork.fetchRoles(rolesToUpdate.toTypedArray())

                    persistFetchedRoles(fetchedRoles)

                    fetchedRoles.forEach {
                        roles[it.id] = it
                    }

                    entities.forEach {
                        if (roles.containsKey(it.author?.roleId))
                            it.authorRole = roles[it.author!!.roleId!!]!!
                    }
                }
            }, GlobalScope.launch  {
                if (classesToUpdate.isNotEmpty()) {
                    val fetchedClasses =
                        appNetwork.fetchClasses(classesToUpdate.toTypedArray())

                    persistFetchedClasses(fetchedClasses)

                    fetchedClasses.forEach {
                        classes[it.id] = it
                    }

                    entities.forEach {
                        if (classes.containsKey(it.author?.classId))
                            it.authorClass = classes[it.author!!.classId!!]!!
                    }
                }
            }).joinAll()
        }
    }

    private suspend fun populateAnnouncementEntity(
        entity: AnnouncementEntity,
        updated: DeferredAnnouncementInfo = DeferredAnnouncementInfo(
            HashMap(),
            HashMap(),
            HashMap()
        ),
        forceFetch: UpdateParameters = UpdateParameters.DETERMINE
    ) {
        if (forceFetch == UpdateParameters.DONT_UPDATE) {
            entity.author = userEntityDao.getUser(entity.authorId)
            entity.authorRole = if (entity.author?.roleId != null) {
                roleEntityDao.getRole(entity.author!!.roleId!!)
            } else {
                null
            }
            entity.authorClass = if (entity.author?.classId != null) {
                classEntityDao.getClass(entity.author!!.classId!!)
            } else {
                null
            }
        } else {
            val oneDayBefore = ZonedDateTime.now().minusDays(1)
            if (forceFetch == UpdateParameters.UPDATE) {
                entity.author = userEntityDao
                    .getUser(entity.authorId)
            }

            if (
                forceFetch == UpdateParameters.UPDATE ||
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
                    if (forceFetch == UpdateParameters.DETERMINE) {
                        entity.authorRole = roleEntityDao.getRole(
                            entity.author!!.roleId!!
                        )
                    }

                    if (
                        forceFetch == UpdateParameters.UPDATE ||
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
                    if (forceFetch == UpdateParameters.DETERMINE) {
                        entity.authorClass = classEntityDao.getClass(
                            entity.author!!.classId!!
                        )
                    }

                    if (
                        forceFetch == UpdateParameters.UPDATE ||
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
    }

    fun pushNewAnnouncement(map: Map<String, String>, notify: Boolean = true) {
        GlobalScope.launch {
            val entity = AnnouncementEntity(
                (map["id"] ?: error("")).toInt(),
                (map["authorId"] ?: error("")).toInt(),
                map["text"] ?: error(""),
                ZonedDateTime.parse(map["createdAt"])
            )

            populateAnnouncementEntity(entity)

            persistFetchedAnnouncement(entity)

            unreadAnnouncements.add(0, entity)
            unreadAnnouncementsPushListener?.invoke(entity)

            if (notify && !isAppForeground) {
                notificationBuilder
                    .setContentTitle(
                        announcementEntityToCaption(
                            entity,
                            context.getString(R.string.author_no_name)
                        )
                    )
                    .setContentText(entity.text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

                try {
                    NotificationManagerCompat.from(context).notify(
                        entity.id,
                        notificationBuilder.build()
                    )
                } catch (e: Exception) {
                    println(e.message)
                }
            }
        }
    }

    suspend fun getAnnouncement(id: Int): AnnouncementEntity? {
        if (user == null) {
            return null
        }

        var announcement: AnnouncementEntity? = announcementEntityDao.getAnnouncement(id)
        val now: ZonedDateTime = ZonedDateTime.now()
        val tenMinutesBefore = now.minusMinutes(10)

        if (
            announcement?.updatedAt?.isAfter(now) != false ||
            announcement.updatedAt!!.isBefore(tenMinutesBefore)
        ) {
            announcement = appNetwork.fetchAnnouncement(user!!.jwt, id)
        }

        if (announcement != null) {
            populateAnnouncementEntity(announcement)
        }

        return announcement
    }

    suspend fun getAnnouncements(
        offset: Int = 0,
        forceFetch: UpdateParameters = UpdateParameters.DETERMINE
    ): Array<AnnouncementEntity> {
        val announcements: Array<AnnouncementEntity>

        if (user == null) {
            return emptyArray()
        }

        val isNeededToUpdate: Boolean = when (forceFetch) {
            UpdateParameters.UPDATE -> true
            UpdateParameters.DETERMINE -> {
                val announcement: AnnouncementEntity? =
                    announcementEntityDao.getAnnouncementAtOffset(offset)
                val now: ZonedDateTime = ZonedDateTime.now()
                val tenMinutesBefore = now.minusMinutes(10)

                announcement?.updatedAt?.isAfter(now) != false ||
                        announcement.updatedAt!!.isBefore(tenMinutesBefore)
            }
            UpdateParameters.DONT_UPDATE -> false
        }

        if (isNeededToUpdate) {
            announcements = appNetwork.fetchAnnouncements(
                user!!.jwt,
                offset
            )

            persistFetchedAnnouncements(announcements)
        } else {
            announcements = announcementEntityDao.getAnnouncements(offset, DEFAULT_LIMIT)
        }

        populateAnnouncements(announcements, forceFetch)

        return announcements
    }

    private suspend fun getRoles(rolesIds: Array<Int>): Array<RoleEntity?> {
        val result: Array<RoleEntity?> = arrayOfNulls(rolesIds.size)
        val now = ZonedDateTime.now()
        val roleIdsToFetch = arrayListOf<Pair<Int, Int>>()

        List(rolesIds.size) {
            GlobalScope.launch {
                val role: RoleEntity? = roleEntityDao.getRole(rolesIds[it])

                if (
                    role?.updatedAt == null ||
                    now <= role.updatedAt!!.plusWeeks(1)
                ) {
                    roleIdsToFetch.add(Pair(rolesIds[it], it))
                }

                result[it] = role
            }
        }.joinAll()

        val fetchedRoles = appNetwork.fetchRoles(Array(
            roleIdsToFetch.size
        ) {
            roleIdsToFetch[it].first
        })

        persistFetchedRoles(fetchedRoles)

        for (i in 0 until roleIdsToFetch.size) {
            result[roleIdsToFetch[i].second] = fetchedRoles[i]
        }

        return result
    }

    private suspend fun getClasses(classesIds: Array<Int>): Array<ClassEntity?> {
        val result: Array<ClassEntity?> = arrayOfNulls(classesIds.size)
        val now = ZonedDateTime.now()
        val classIdsToFetch = arrayListOf<Pair<Int, Int>>()

        List(classesIds.size) {
            GlobalScope.launch {
                val classEntity: ClassEntity? = classEntityDao.getClass(classesIds[it])

                if (
                    classEntity?.updatedAt == null ||
                    now <= classEntity.updatedAt!!.plusWeeks(1)
                ) {
                    classIdsToFetch.add(Pair(classesIds[it], it))
                }

                result[it] = classEntity
            }
        }.joinAll()

        val fetchedClasses = appNetwork.fetchClasses(Array(
            classIdsToFetch.size
        ) {
            classIdsToFetch[it].first
        })

        persistFetchedClasses(fetchedClasses)

        for (i in 0 until classIdsToFetch.size) {
            result[classIdsToFetch[i].second] = fetchedClasses[i]
        }

        return result
    }

    suspend fun getKeyedRoles(rolesIds: Array<Int>): HashMap<Int, RoleEntity> {
        val keyed = HashMap<Int, RoleEntity>()
        val roles = getRoles(rolesIds)

        roles.forEach {
            if (it != null) {
                keyed[it.id] = it
            }
        }

        return keyed
    }

    suspend fun getKeyedClasses(classesIds: Array<Int>): HashMap<Int, ClassEntity> {
        val keyed = HashMap<Int, ClassEntity>()
        val classes = getClasses(classesIds)

        classes.forEach {
            if (it != null) {
                keyed[it.id] = it
            }
        }

        return keyed
    }

    private suspend fun persistFetchedAnnouncement(
        fetchedAnnouncement: AnnouncementEntity
    ) {
        fetchedAnnouncement.updatedAt = ZonedDateTime.now()

        announcementEntityDao.upsert(fetchedAnnouncement)
    }

    private fun persistFetchedAnnouncements(
        fetchedAnnouncements: Array<AnnouncementEntity>
    ) {
        val now = ZonedDateTime.now()

        fetchedAnnouncements.forEach {
            it.updatedAt = now
        }

        GlobalScope.launch(Dispatchers.IO) {
            announcementEntityDao.upsertArray(fetchedAnnouncements)
        }
    }

    private fun persistFetchedUser(fetchedUser: UserEntity) {
        fetchedUser.updatedAt = ZonedDateTime.now()

        GlobalScope.launch(Dispatchers.IO) {
            userEntityDao.upsert(fetchedUser)
        }
    }

    private fun persistFetchedUsers(fetchedUsers: Array<UserEntity>) {
        val now = ZonedDateTime.now()

        fetchedUsers.forEach {
            it.updatedAt = now
        }

        GlobalScope.launch(Dispatchers.IO) {
            userEntityDao.upsertArray(fetchedUsers)
        }
    }

    private fun persistFetchedRole(fetchedRole: RoleEntity) {
        fetchedRole.updatedAt = ZonedDateTime.now()

        GlobalScope.launch(Dispatchers.IO) {
            roleEntityDao.upsert(fetchedRole)
        }
    }

    private fun persistFetchedRoles(fetchedRoles: Array<RoleEntity>) {
        val now = ZonedDateTime.now()

        fetchedRoles.forEach {
            it.updatedAt = now
        }

        GlobalScope.launch(Dispatchers.IO) {
            roleEntityDao.upsertArray(fetchedRoles)
        }
    }

    private fun persistFetchedClass(fetchedClass: ClassEntity) {
        fetchedClass.updatedAt = ZonedDateTime.now()

        GlobalScope.launch(Dispatchers.IO) {
            classEntityDao.upsert(fetchedClass)
        }
    }

    private fun persistFetchedClasses(fetchedClasses: Array<ClassEntity>) {
        val now = ZonedDateTime.now()

        fetchedClasses.forEach {
            it.updatedAt = now
        }

        GlobalScope.launch(Dispatchers.IO) {
            classEntityDao.upsertArray(fetchedClasses)
        }
    }
}