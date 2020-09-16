package ru.labore.moderngymnasium.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import org.threeten.bp.ZonedDateTime

@Entity(tableName = "role")
data class RoleEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,

    @SerializedName("name_ru")
    val nameRu: String,

    val type: String,
    val name: String,
    var updatedAt: ZonedDateTime? = null
)