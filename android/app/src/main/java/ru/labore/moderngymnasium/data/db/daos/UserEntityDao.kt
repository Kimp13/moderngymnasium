package ru.labore.moderngymnasium.data.db.daos

import androidx.room.*
import ru.labore.moderngymnasium.data.db.entities.UserEntity

@Dao
interface UserEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(user: UserEntity)

    @Query("select updatedAt from user where id = :userId")
    suspend fun getUserLastUpdatedTime(userId: Int): UserEntity?
}
