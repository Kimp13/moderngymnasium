package ru.labore.moderngymnasium.data.db.entities

import androidx.room.Entity
import org.threeten.bp.ZonedDateTime

@Entity(tableName = "comment")
class CommentEntity(
    id: Int,

    authorId: Int,
    commentCount: Int,
    val announcementId: Int,
    val replyTo: Int?,
    text: String,
    val childrenCount: Int,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime
) : AuthoredEntity(
    id,
    authorId,
    commentCount,
    text,
    createdAt,
    updatedAt
)