package ru.labore.eventeger.ui.fragments.inbox

import android.app.Activity
import android.app.Application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.labore.eventeger.data.AppRepository
import ru.labore.eventeger.data.db.entities.AnnouncementEntity
import ru.labore.eventeger.ui.adapters.InboxRecyclerViewAdapter
import ru.labore.eventeger.ui.base.BaseRecyclerViewModel

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
        announcementClick: (AnnouncementEntity) -> Unit,
        createButtonClick: () -> Unit
    ) = InboxRecyclerViewAdapter(this).also {
        onAnnouncementClick = announcementClick
        onCreateButtonClick = createButtonClick

        adapter = it
    }

    suspend fun updateAnnouncements(
        activity: Activity,
        forceFetch: AppRepository.Companion.UpdateParameters =
            AppRepository.Companion.UpdateParameters.DETERMINE,
        refresh: Boolean = false
    ) {
        if (current == null || !current!!.isActive) {
            if (refresh || !reachedEnd) {
                loading = true

                val offset = if (refresh)
                    0
                else
                    currentOffset

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

                println(newAnnouncements.values.size)

                if (refresh) {
                    val previousSize = itemCount

                    reachedEnd = false
                    items.clear()
                    items.addAll(newAnnouncements.values.sortedByDescending {
                        it.createdAt
                    })
                    currentOffset = items.size

                    refreshItems(
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
                        items.addAll(newAnnouncements.values.sortedByDescending {
                            it.createdAt
                        })

                        pushItems(
                            previousSize,
                            newAnnouncements.size
                        )
                    }
                }
            }
        } else {
            current?.join()
        }
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