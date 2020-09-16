package ru.labore.moderngymnasium.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.ZonedDateTime

@Entity(tableName = "class")
data class ClassEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,

    val name: String,
    var updatedAt: ZonedDateTime? = null
)