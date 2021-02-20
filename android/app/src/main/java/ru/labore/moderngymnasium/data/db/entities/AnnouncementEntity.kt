package ru.labore.moderngymnasium.data.db.entities

import androidx.room.Entity
import org.threeten.bp.ZonedDateTime

@Entity(tableName = "announcement")
class AnnouncementEntity(
    id: Int,
    authorId: Int,
    val text: String,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
    var startsAt: ZonedDateTime? = null,
    var endsAt: ZonedDateTime? = null
) : AuthoredEntity(
    id,
    authorId,
    createdAt,
    updatedAt
)