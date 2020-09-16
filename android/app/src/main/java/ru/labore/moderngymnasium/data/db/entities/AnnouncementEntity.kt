package ru.labore.moderngymnasium.data.db.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import org.threeten.bp.ZonedDateTime

@Entity(tableName = "announcement")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,

    @SerializedName("created_at")
    val createdAt: Int,

    @SerializedName("author_id")
    val authorId: Int,

    val text: String,
    var updatedAt: ZonedDateTime? = null
) {
    @Ignore
    var author: UserEntity? = null

    @Ignore
    var authorRole: RoleEntity? = null

    @Ignore
    var authorClass: ClassEntity? = null
}