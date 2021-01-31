package ru.labore.moderngymnasium.ui.fragments.inbox

import android.app.Application
import org.threeten.bp.ZonedDateTime
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.adapters.InboxRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.BaseViewModel
import ru.labore.moderngymnasium.ui.base.ListElementFragment
import ru.labore.moderngymnasium.ui.fragments.create.CreateFragment
import ru.labore.moderngymnasium.ui.fragments.detailedAnnouncement.DetailedAnnouncementFragment

class InboxViewModel(
    private val app: Application
) : BaseViewModel(app) {
    private lateinit var viewAdapter: InboxRecyclerViewAdapter
    private var currentOffset = appRepository.now()
    val itemCount
        get() = announcements.size


    private var reachedEnd = false
    private val announcements = mutableListOf<AnnouncementEntity>()

    init {
        appRepository.unreadAnnouncementsPushListener = {
            announcements.add(InboxRecyclerViewAdapter.additionalItems, it)
            viewAdapter.prependAnnouncement()
        }
    }

    fun getAdapter(
        controls: ListElementFragment.Companion.ListElementFragmentControls
    ): InboxRecyclerViewAdapter {
        viewAdapter = InboxRecyclerViewAdapter(
            app.resources,
            announcements,
            {
                controls.push(CreateFragment(controls))
            },
            {
                controls.push(DetailedAnnouncementFragment(controls, it))
            }
        )

        return viewAdapter
    }

    suspend fun updateAnnouncements(
        forceFetch: AppRepository.Companion.UpdateParameters =
            AppRepository.Companion.UpdateParameters.DETERMINE,
        refresh: Boolean = false
    ) {
        if (refresh || !reachedEnd) {
            val offset = if (refresh) {
                appRepository.now()
            } else {
                currentOffset
            }

            val newAnnouncements = getAnnouncements(offset, forceFetch)

            if (newAnnouncements.isNotEmpty())
                currentOffset = newAnnouncements.last().createdAt

            if (refresh) {
                val previousSize = itemCount

                reachedEnd = false
                announcements.clear()
                announcements.addAll(newAnnouncements)

                viewAdapter.refreshAnnouncements(
                    previousSize,
                    itemCount
                )
            } else {
                if (newAnnouncements.isEmpty()) {
                    reachedEnd = true
                } else {
                    val previousSize = itemCount

                    announcements.addAll(newAnnouncements)

                    viewAdapter.pushAnnouncements(
                        previousSize,
                        itemCount
                    )
                }
            }
        }
    }

    private suspend fun getAnnouncements(
        offset: ZonedDateTime,
        forceFetch: AppRepository.Companion.UpdateParameters
    ) =
        appRepository.getAnnouncements(offset, forceFetch)
}