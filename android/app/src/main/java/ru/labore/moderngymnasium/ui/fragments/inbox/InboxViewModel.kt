package ru.labore.moderngymnasium.ui.fragments.inbox

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.activities.AnnouncementDetailedActivity
import ru.labore.moderngymnasium.ui.activities.MainActivity
import ru.labore.moderngymnasium.ui.adapters.InboxRecyclerViewAdapter

class InboxViewModel(
    application: Application
) : AndroidViewModel(application), DIAware {
    override val di: DI by lazy { (application as DIAware).di }
    val appRepository: AppRepository by instance()
    val viewAdapter = InboxRecyclerViewAdapter(
        application.resources,
        mutableListOf()
    ) {
        val intent = Intent(
            application.applicationContext,
            AnnouncementDetailedActivity::class.java
        )
        val bundle = Bundle()
        bundle.putParcelable("announcement", it)
        intent.putExtras(bundle)
        application.startActivity(intent)
    }
    val itemCount
        get() = viewAdapter.announcements.size


    private var reachedEnd = false
    var adapterBinded = false

    init {
        appRepository.unreadAnnouncementsPushListener = {
            viewAdapter.prependAnnouncement(it)
        }
    }

    fun cleanseUser() {
        appRepository.user = null
    }

    fun bindAdapter(): InboxRecyclerViewAdapter {
        if (!adapterBinded) {
            adapterBinded = true
        }

        return viewAdapter
    }

    suspend fun updateAnnouncements(
        forceFetch: AppRepository.Companion.UpdateParameters =
            AppRepository.Companion.UpdateParameters.DETERMINE,
        refresh: Boolean = false
    ) {
        if (refresh || !reachedEnd) {
            val offset = if (refresh) {
                0
            } else {
                itemCount
            }

            val announcements = getAnnouncements(offset, forceFetch)

            if (refresh) {
                reachedEnd = false
                viewAdapter.refreshAnnouncements(announcements)
            } else {
                if (announcements.isEmpty()) {
                    reachedEnd = true
                } else {
                    viewAdapter.pushAnnouncements(announcements)
                }
            }
        }
    }

    suspend fun getAnnouncements(
        offset: Int,
        forceFetch: AppRepository.Companion.UpdateParameters
    ) =
        appRepository.getAnnouncements(offset, forceFetch)
}