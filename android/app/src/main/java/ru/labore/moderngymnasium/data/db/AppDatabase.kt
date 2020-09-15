package ru.labore.moderngymnasium.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.labore.moderngymnasium.data.db.daos.AnnouncementEntityDao
import ru.labore.moderngymnasium.data.db.daos.RoleEntityDao
import ru.labore.moderngymnasium.data.db.daos.UserEntityDao
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.data.db.entities.RoleEntity
import ru.labore.moderngymnasium.data.db.entities.UserEntity

@Database(
    entities = [AnnouncementEntity::class, RoleEntity::class, UserEntity::class],
    version = 1
)
@TypeConverters(DateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun announcementEntityDao() : AnnouncementEntityDao
    abstract fun userEntityDao() : UserEntityDao
    abstract fun roleEntityDao() : RoleEntityDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moderngymnasium.db"
                )
                .build()
    }
}