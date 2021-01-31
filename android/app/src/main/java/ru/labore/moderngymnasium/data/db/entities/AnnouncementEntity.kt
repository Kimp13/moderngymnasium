package ru.labore.moderngymnasium.data.db.entities

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import org.threeten.bp.ZonedDateTime

@Entity(tableName = "announcement")
class AnnouncementEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,

    val authorId: Int,
    val text: String,
    var createdAt: ZonedDateTime,

    var updatedAt: ZonedDateTime
) {
    @Ignore
    var author: UserEntity? = null

    @Ignore
    var authorRole: RoleEntity? = null

    @Ignore
    var authorClass: ClassEntity? = null
}