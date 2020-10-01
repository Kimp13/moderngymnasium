package ru.labore.moderngymnasium

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import org.kodein.di.*
import org.kodein.di.android.x.androidXModule
import ru.labore.moderngymnasium.data.db.AppDatabase
import ru.labore.moderngymnasium.data.network.AppNetwork
import ru.labore.moderngymnasium.data.repository.AppRepository

class MGApplication : Application(), DIAware {
    override val di = DI.lazy {
        import(androidXModule(this@MGApplication))

        bind() from singleton { AppDatabase(instance()) }
        bind() from singleton { instance<AppDatabase>().announcementEntityDao() }
        bind() from singleton { instance<AppDatabase>().userEntityDao() }
        bind() from singleton { instance<AppDatabase>().roleEntityDao() }
        bind() from singleton { instance<AppDatabase>().classEntityDao() }
        bind() from singleton { AppNetwork(instance()) }
        bind() from singleton { AppRepository(
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance()
        ) }
    }

    override fun onCreate() {
        super.onCreate()

        AndroidThreeTen.init(this)
    }
}