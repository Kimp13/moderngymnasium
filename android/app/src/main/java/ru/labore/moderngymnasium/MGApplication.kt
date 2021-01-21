package ru.labore.moderngymnasium

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.GsonBuilder
import com.jakewharton.threetenabp.AndroidThreeTen
import org.kodein.di.*
import org.kodein.di.android.x.androidXModule
import org.threeten.bp.ZonedDateTime
import ru.labore.moderngymnasium.data.db.AppDatabase
import ru.labore.moderngymnasium.data.network.AppNetwork
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.data.sharedpreferences.entities.AllPermissions
import ru.labore.moderngymnasium.data.sharedpreferences.entities.AnnounceMap
import ru.labore.moderngymnasium.utils.*

class MGApplication : Application(), DIAware, LifecycleObserver {
    override val di = DI.lazy {
        val gson = GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, JsonDateSerializerImpl())
            .registerTypeAdapter(ZonedDateTime::class.java, JsonDateDeserializerImpl())
            .registerTypeAdapter(AllPermissions::class.java, JsonPermissionsSerializerImpl())
            .registerTypeAdapter(AllPermissions::class.java, JsonPermissionsDeserializerImpl())
            .registerTypeAdapter(AnnounceMap::class.java, JsonAnnounceMapSerializerImpl())
            .registerTypeAdapter(AnnounceMap::class.java, JsonAnnounceMapDeserializerImpl())
            .create()

        import(androidXModule(this@MGApplication))

        bind() from singleton { AppDatabase(instance()) }
        bind() from singleton { instance<AppDatabase>().announcementEntityDao() }
        bind() from singleton { instance<AppDatabase>().userEntityDao() }
        bind() from singleton { instance<AppDatabase>().roleEntityDao() }
        bind() from singleton { instance<AppDatabase>().classEntityDao() }
        bind() from singleton { AppNetwork(instance(), gson) }
        bind() from singleton { AppRepository(
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            gson
        ) }
    }
    private val repository: AppRepository by instance()

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        AndroidThreeTen.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.description = getString(R.string.notification_channel_description)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        repository.isAppForeground = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        repository.isAppForeground = true
    }
}