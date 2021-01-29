package ru.labore.moderngymnasium.ui.fragments.inbox

import android.app.Application
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.adapters.InboxRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.BaseViewModel
import ru.labore.moderngymnasium.ui.base.ListElementFragment
import ru.labore.moderngymnasium.ui.fragments.create.CreateFragment
import ru.labore.moderngymnasium.ui.fragments.detailedAnnouncement.DetailedAnnouncementFragment

class InboxViewModel(
    application: Application
) : BaseViewModel(application) {
    private lateinit var viewAdapter: InboxRecyclerViewAdapter
    val itemCount
        get() = viewAdapter.announcements.size


    private var reachedEnd = false
    private var adapterBinded = false

    init {
        appRepository.unreadAnnouncementsPushListener = {
            viewAdapter.prependAnnouncement(it)
        }
    }

    fun bindAdapter(
        controls: ListElementFragment.Companion.ListElementFragmentControls
    ): InboxRecyclerViewAdapter {
        if (!adapterBinded) {
            val application = getApplication<Application>()

            viewAdapter = InboxRecyclerViewAdapter(
                application.resources,
                {
                    controls.push(CreateFragment(controls))
                },
                {
                    controls.push(DetailedAnnouncementFragment(controls, it))
                }
            )
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

    private suspend fun getAnnouncements(
        offset: Int,
        forceFetch: AppRepository.Companion.UpdateParameters
    ) =
        appRepository.getAnnouncements(offset, forceFetch)
}