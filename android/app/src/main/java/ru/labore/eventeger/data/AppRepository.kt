package ru.labore.eventeger.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import ru.labore.eventeger.R
import ru.labore.eventeger.data.db.daos.*
import ru.labore.eventeger.data.db.entities.*
import ru.labore.eventeger.data.network.AppNetwork
import ru.labore.eventeger.data.sharedpreferences.entities.AnnounceMap
import ru.labore.eventeger.data.sharedpreferences.entities.User
import ru.labore.eventeger.utils.announcementEntityToCaption

class AppRepository(
    private val context: Context,
    private val commentEntityDao: CommentEntityDao,
    private val announcementEntityDao: AnnouncementEntityDao,
    private val userEntityDao: UserEntityDao,
    private val roleEntityDao: RoleEntityDao,
    private val classEntityDao: ClassEntityDao,
    val appNetwork: AppNetwork,
    private val gson: Gson
) {
    companion object {
        const val DEFAULT_LIMIT = 25

        const val HTTP_RESPONSE_CODE_UNAUTHORIZED = 401

        enum class UpdateParameters {
            UPDATE, DONT_UPDATE, DETERMINE
        }
    }

    var isAppForeground: Boolean = true
    var user: User? = null
    var announceMap = AnnounceMap()
    var unreadAnnouncements: MutableList<AnnouncementEntity> = mutableListOf()
    var unreadAnnouncementsPushListener: ((AnnouncementEntity) -> Unit)? = null

    val users = hashMapOf<Int, UserEntity>()
    val roles = hashMapOf<Int, RoleEntity>()
    val classes = hashMapOf<Int, ClassEntity>()
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

    fun now(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))!!
    fun zonedNow(): ZonedDateTime = ZonedDateTime.now()!!

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

    suspend fun createComment(
        announcementId: Int,
        text: String,
        hidden: Boolean,
        replyTo: Int? = null
    ) = appNetwork.createComment(
        user!!.jwt,
        announcementId,
        text,
        hidden,
        replyTo
    )

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

    suspend fun createAnnouncement(
        text: String,
        recipients: HashMap<Int, HashSet<Int>>,
        beginsAt: ZonedDateTime?,
        endsAt: ZonedDateTime?
    ) {
        if (user?.jwt != null) {
            appNetwork.createAnnouncement(
                user!!.jwt,
                text,
                recipients,
                beginsAt,
                endsAt
            )
        }
    }

    private suspend fun populateAuthoredEntities(
        entities: Array<out AuthoredEntity>,
        forceFetch: UpdateParameters = UpdateParameters.DETERMINE
    ) {
        val now = now()
        val rolesToUpdate = HashSet<Int>()
        val classesToUpdate = HashSet<Int>()
        val usersToUpdate = HashSet<Int>()

        when (forceFetch) {
            UpdateParameters.UPDATE -> entities.forEach {
                usersToUpdate.add(it.authorId)
            }
            UpdateParameters.DETERMINE -> entities.forEach {
                val author: UserEntity? = if (users.contains(it.authorId)) {
                    users[it.authorId]
                } else {
                    val queried = userEntityDao.getUser(it.authorId)

                    if (queried != null)
                        users[it.authorId] = queried

                    queried
                }


                if (
                    author?.updatedAt == null ||
                    now <= author.updatedAt!!.plusWeeks(1)
                )
                    usersToUpdate.add(it.authorId)
            }
            else -> entities.forEach {
                if (!users.contains(it.authorId)) {
                    val queried = userEntityDao.getUser(it.authorId)

                    if (queried != null)
                        users[it.authorId] = queried
                }
            }
        }

        if (forceFetch != UpdateParameters.DONT_UPDATE && usersToUpdate.isNotEmpty()) {
            val fetchedUsers = appNetwork.fetchUsers(usersToUpdate.toTypedArray())

            fetchedUsers.forEach {
                users[it.id] = it
            }

            persistFetchedUsers(fetchedUsers)
        }

        when (forceFetch) {
            UpdateParameters.UPDATE -> entities.forEach {
                users[it.authorId]?.let { user ->
                    if (user.roleId != null)
                        rolesToUpdate.add(user.roleId!!)

                    if (user.classId != null)
                        classesToUpdate.add(user.classId!!)
                }
            }
            UpdateParameters.DETERMINE -> entities.forEach {
                val user = users[it.authorId]

                if (user != null) {
                    if (user.roleId != null && roles[user.roleId] == null) {
                        val queried = roleEntityDao.getRole(user.roleId!!)

                        if (queried != null)
                            roles[user.roleId!!] = queried
                    }

                    if (user.classId != null && classes[user.classId] == null) {
                        val queried = classEntityDao.getClass(user.classId!!)

                        if (queried != null)
                            classes[user.classId!!] = queried
                    }

                    val role = roles[user.roleId]
                    val `class` = classes[user.classId]

                    if (
                        (
                                role?.updatedAt == null ||
                                        now < role.updatedAt!!.plusWeeks(1)
                                ) &&
                        user.roleId != null
                    )
                        rolesToUpdate.add(user.roleId!!)

                    if (
                        (
                                `class`?.updatedAt == null ||
                                        now < `class`.updatedAt!!.plusWeeks(1)
                                ) &&
                        user.classId != null
                    )
                        classesToUpdate.add(user.classId!!)
                }
            }
            else -> entities.forEach {
                val user = users[it.authorId]

                if (user != null) {
                    if (user.roleId != null && roles[user.roleId] == null) {
                        val queried = roleEntityDao.getRole(user.roleId!!)

                        if (queried != null)
                            roles[user.roleId!!] = queried
                    }

                    if (user.classId != null && classes[user.classId] == null) {
                        val queried = classEntityDao.getClass(user.classId!!)

                        if (queried != null)
                            classes[user.classId!!] = queried
                    }
                }
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
                }
            }, GlobalScope.launch {
                if (classesToUpdate.isNotEmpty()) {
                    val fetchedClasses =
                        appNetwork.fetchClasses(classesToUpdate.toTypedArray())

                    persistFetchedClasses(fetchedClasses)

                    fetchedClasses.forEach {
                        classes[it.id] = it
                    }
                }
            }).joinAll()
        }
    }

    private suspend fun populateAnnouncementEntity(
        entity: AnnouncementEntity,
        forceFetch: UpdateParameters = UpdateParameters.DETERMINE
    ) {
        val queriedUser = userEntityDao.getUser(entity.authorId)
        var queriedRole: RoleEntity? = null
        var queriedClass: ClassEntity? = null

        if (queriedUser != null) {
            if (queriedUser.roleId != null) {
                queriedRole = roleEntityDao.getRole(queriedUser.roleId!!)

                if (queriedRole != null && !roles.contains(queriedRole.id))
                    roles[queriedRole.id] = queriedRole
            }

            if (queriedUser.classId != null) {
                queriedClass = classEntityDao.getClass(queriedUser.classId!!)

                if (queriedClass != null && !classes.contains(queriedClass.id))
                    classes[queriedClass.id] = queriedClass
            }
        }

        if (forceFetch != UpdateParameters.DONT_UPDATE) {
            val oneDayBefore = now().minusDays(1)

            if (
                queriedUser?.updatedAt?.isBefore(oneDayBefore) != false ||
                forceFetch == UpdateParameters.UPDATE
            ) {
                val oneWeekBefore = oneDayBefore.minusDays(6)
                val fetched = appNetwork.fetchUser(entity.authorId)

                if (fetched != null) {
                    persistFetchedUser(fetched)

                    users[entity.authorId] = fetched

                    if (
                        fetched.roleId != null &&
                        (
                                queriedRole?.updatedAt?.isBefore(oneWeekBefore) != false ||
                                        forceFetch == UpdateParameters.UPDATE
                                )
                    ) {
                        val fetchedRole = appNetwork.fetchRole(fetched.roleId!!)

                        if (fetchedRole != null) {
                            persistFetchedRole(fetchedRole)

                            roles[fetchedRole.id] = fetchedRole
                        }
                    }

                    if (
                        fetched.classId != null &&
                        (
                                queriedClass?.updatedAt?.isBefore(oneWeekBefore) != false ||
                                        forceFetch == UpdateParameters.UPDATE
                                )
                    ) {
                        val fetchedClass = appNetwork.fetchClass(fetched.classId!!)

                        if (fetchedClass != null) {
                            persistFetchedClass(fetchedClass)

                            classes[fetchedClass.id] = fetchedClass
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
                0,
                map["text"] ?: error(""),
                ZonedDateTime.parse(map["createdAt"]),
                now(),
                map["isEvent"].toBoolean(),
            )

            populateAnnouncementEntity(entity)

            persistFetchedAnnouncement(entity)

            unreadAnnouncements.add(0, entity)
            unreadAnnouncementsPushListener?.invoke(entity)

            if (notify) {
                notificationBuilder
                    .setContentTitle(
                        announcementEntityToCaption(
                            users[entity.authorId],
                            context.getString(R.string.noname)
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

    suspend fun getComments(
        announcementId: Int,
        offset: Int,
        forceFetch: UpdateParameters,
        replyTo: Int? = null
    ): Array<CommentEntity> {
        if (user == null)
            return emptyArray()

        println("$announcementId, $offset, $forceFetch, $replyTo")

        val comments: Array<CommentEntity>

        val isNeededToUpdate: Boolean = when (forceFetch) {
            UpdateParameters.UPDATE -> true
            UpdateParameters.DETERMINE -> {
                val comment = if (replyTo == null)
                    commentEntityDao.getCommentAtOffset(announcementId, offset)
                else
                    commentEntityDao.getCommentAtOffset(announcementId, offset, replyTo)

                val now = now()
                val hourBefore = now.minusHours(1)

                comment?.createdAt?.isAfter(now) != false ||
                        comment.createdAt.isBefore(hourBefore)
            }
            else -> false
        }

        if (isNeededToUpdate) {
            comments = appNetwork.fetchComments(
                user!!.jwt,
                announcementId,
                offset,
                replyTo
            )

            persistFetchedComments(comments)
        } else {
            comments = if (replyTo == null)
                commentEntityDao.getComments(announcementId, offset, DEFAULT_LIMIT)
            else
                commentEntityDao.getComments(announcementId, offset, DEFAULT_LIMIT, replyTo)
        }

        populateAuthoredEntities(comments, forceFetch)

        return comments
    }

    suspend fun getAnnouncements(
        offset: Int = 0,
        forceFetch: UpdateParameters = UpdateParameters.DETERMINE
    ): Array<AnnouncementEntity> {
        if (user == null)
            return emptyArray()

        val announcements: Array<AnnouncementEntity>

        val isNeededToUpdate: Boolean = when (forceFetch) {
            UpdateParameters.UPDATE -> true
            UpdateParameters.DETERMINE -> {
                val announcement =
                    announcementEntityDao.getAnnouncementAtOffset(offset)
                val now = now()
                val tenMinutesBefore = now.minusMinutes(10)

                announcement?.createdAt?.isAfter(now) != false ||
                        announcement.createdAt.isBefore(tenMinutesBefore)
            }
            UpdateParameters.DONT_UPDATE -> false
        }

        when {
            isNeededToUpdate -> {
                announcements = appNetwork.fetchAnnouncements(
                    user!!.jwt,
                    offset
                )

                persistFetchedAnnouncements(announcements)
            }
            else -> {
                announcements = announcementEntityDao.getAnnouncements(offset, DEFAULT_LIMIT)
            }
        }

        populateAuthoredEntities(announcements, forceFetch)

        return announcements
    }

    private suspend fun getRoles(rolesIds: Array<Int>): Array<RoleEntity?> {
        val result: Array<RoleEntity?> = arrayOfNulls(rolesIds.size)
        val now = now()
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
        val now = now()
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

    suspend fun persistFetchedAuthoredEntity(
        item: AuthoredEntity
    ) = when (item) {
        is AnnouncementEntity -> persistFetchedAnnouncement(item)
        is CommentEntity -> persistFetchedComment(item)
        else -> TODO()
    }

    private suspend fun persistFetchedComment(
        fetchedComment: CommentEntity
    ) {
        fetchedComment.updatedAt = now()

        commentEntityDao.upsert(fetchedComment)
    }

    private suspend fun persistFetchedComments(
        fetchedComments: Array<CommentEntity>
    ) {
        val now = now()

        fetchedComments.forEach {
            it.updatedAt = now
        }

        commentEntityDao.upsertArray(fetchedComments)
    }

    suspend fun persistFetchedAnnouncement(
        fetchedAnnouncement: AnnouncementEntity
    ) {
        fetchedAnnouncement.updatedAt = now()

        announcementEntityDao.upsert(fetchedAnnouncement)
    }

    private fun persistFetchedAnnouncements(
        fetchedAnnouncements: Array<AnnouncementEntity>
    ) {
        val now = now()

        fetchedAnnouncements.forEach {
            it.updatedAt = now
        }

        GlobalScope.launch(Dispatchers.IO) {
            announcementEntityDao.upsertArray(fetchedAnnouncements)
        }
    }

    private fun persistFetchedUser(fetchedUser: UserEntity) {
        fetchedUser.updatedAt = now()

        GlobalScope.launch(Dispatchers.IO) {
            userEntityDao.upsert(fetchedUser)
        }
    }

    private fun persistFetchedUsers(fetchedUsers: Array<UserEntity>) {
        val now = now()

        fetchedUsers.forEach {
            it.updatedAt = now
        }

        GlobalScope.launch(Dispatchers.IO) {
            userEntityDao.upsertArray(fetchedUsers)
        }
    }

    private fun persistFetchedRole(fetchedRole: RoleEntity) {
        fetchedRole.updatedAt = now()

        GlobalScope.launch(Dispatchers.IO) {
            roleEntityDao.upsert(fetchedRole)
        }
    }

    private fun persistFetchedRoles(fetchedRoles: Array<RoleEntity>) {
        val now = now()

        fetchedRoles.forEach {
            it.updatedAt = now
        }

        GlobalScope.launch(Dispatchers.IO) {
            roleEntityDao.upsertArray(fetchedRoles)
        }
    }

    private fun persistFetchedClass(fetchedClass: ClassEntity) {
        fetchedClass.updatedAt = now()

        GlobalScope.launch(Dispatchers.IO) {
            classEntityDao.upsert(fetchedClass)
        }
    }

    private fun persistFetchedClasses(fetchedClasses: Array<ClassEntity>) {
        val now = now()

        fetchedClasses.forEach {
            it.updatedAt = now
        }

        GlobalScope.launch(Dispatchers.IO) {
            classEntityDao.upsertArray(fetchedClasses)
        }
    }
}