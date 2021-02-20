package ru.labore.moderngymnasium.data.db.entities

import androidx.room.PrimaryKey
import org.threeten.bp.ZonedDateTime

abstract class AuthoredEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val authorId: Int,
    var createdAt: ZonedDateTime,
    var updatedAt: ZonedDateTime
)