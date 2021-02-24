package ru.labore.moderngymnasium.ui.fragments.inbox

import android.app.Activity
import android.app.Application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.labore.moderngymnasium.data.AppRepository
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.ui.adapters.InboxRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.BaseRecyclerViewModel
import ru.labore.moderngymnasium.ui.base.ListElementFragment
import ru.labore.moderngymnasium.ui.fragments.create.CreateFragment
import ru.labore.moderngymnasium.ui.fragments.detailedAnnouncement.DetailedAnnouncementFragment

class InboxViewModel(
    app: Application
) : BaseRecyclerViewModel(app) {
    init {
        appRepository.unreadAnnouncementsPushListener = {
            items.add(adapter.beginAdditionalItems.size, it)
            adapter.prependItem()
        }
    }

    lateinit var onAnnouncementClick: (AnnouncementEntity) -> Unit
    lateinit var onCreateButtonClick: () -> Unit

    fun getAdapter(
        controls: ListElementFragment.Companion.ListElementFragmentControls
    ): InboxRecyclerViewAdapter {
        val newAdapter = InboxRecyclerViewAdapter(
            this
        )

        onAnnouncementClick = {
            controls.push(DetailedAnnouncementFragment(controls, it))
        }

        onCreateButtonClick = {
            controls.push(CreateFragment(controls))
        }

        adapter = newAdapter

        return newAdapter
    }

    suspend fun updateAnnouncements(
        activity: Activity,
        forceFetch: AppRepository.Companion.UpdateParameters =
            AppRepository.Companion.UpdateParameters.DETERMINE,
        refresh: Boolean = false
    ) {
        loading = true

        if (current == null || !current!!.isActive) {
            if (refresh || !reachedEnd) {
                val offset = if (refresh) {
                    0
                } else {
                    currentOffset
                }

                val newAnnouncements = hashMapOf<Int, AnnouncementEntity>()

                current = GlobalScope.launch {
                    makeRequest(
                        activity,
                        {
                            getAnnouncements(offset, forceFetch).forEach {
                                newAnnouncements[it.id] = it
                            }
                        },
                        {
                            getAnnouncements(
                                offset,
                                AppRepository.Companion.UpdateParameters.DONT_UPDATE
                            ).forEach {
                                newAnnouncements[it.id] = it
                            }
                        }
                    )
                }

                current?.join()

                if (refresh) {
                    val previousSize = itemCount

                    currentOffset = 0
                    reachedEnd = false
                    items.clear()
                    items.addAll(newAnnouncements.values)

                    adapter.refreshItems(
                        previousSize,
                        itemCount
                    )
                } else {
                    if (newAnnouncements.isEmpty()) {
                        reachedEnd = true
                    } else {
                        val iterator = items.listIterator()

                        while (iterator.hasNext()) {
                            val it = iterator.next()
                            val newValue = newAnnouncements[it.id]

                            if (newValue != null) {
                                iterator.set(newValue)
                                newAnnouncements.remove(it.id)
                            }
                        }

                        val previousSize = itemCount

                        currentOffset += newAnnouncements.size
                        items.addAll(newAnnouncements.values)

                        adapter.pushItems(
                            previousSize,
                            newAnnouncements.size
                        )
                    }
                }
            }
        } else {
            current?.join()
        }

        loading = false
    }

    suspend fun setup(activity: Activity) {
        if (itemCount == 0)
            updateAnnouncements(
                activity,
                AppRepository.Companion.UpdateParameters.DETERMINE,
                true
            )
        else
            loading = false
    }

    private suspend fun getAnnouncements(
        offset: Int,
        forceFetch: AppRepository.Companion.UpdateParameters
    ) =
        appRepository.getAnnouncements(offset, forceFetch)
}