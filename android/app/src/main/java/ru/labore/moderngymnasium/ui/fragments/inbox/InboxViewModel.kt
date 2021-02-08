package ru.labore.moderngymnasium.ui.fragments.inbox

import android.app.Activity
import android.app.Application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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

    private var current: Job? = null
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
        activity: Activity,
        forceFetch: AppRepository.Companion.UpdateParameters =
            AppRepository.Companion.UpdateParameters.DETERMINE,
        refresh: Boolean = false
    ) {
        if (current == null || !current!!.isActive) {
            if (refresh || !reachedEnd) {
                val offset = if (refresh) {
                    null
                } else {
                    currentOffset
                }

                var newAnnouncements = arrayOf<AnnouncementEntity>()

                current = GlobalScope.async {
                    makeRequest(
                        activity,
                        {
                            newAnnouncements = getAnnouncements(offset, forceFetch)
                        },
                        {
                            newAnnouncements = getAnnouncements(
                                offset,
                                AppRepository.Companion.UpdateParameters.DONT_UPDATE
                            )
                        }
                    )
                }

                current?.join()

                println(newAnnouncements.size)

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
        } else {
            current?.join()
        }
    }

    private suspend fun getAnnouncements(
        offset: ZonedDateTime?,
        forceFetch: AppRepository.Companion.UpdateParameters
    ) =
        appRepository.getAnnouncements(offset, forceFetch)
}