package ru.labore.moderngymnasium.ui.fragments.detailedComment

import android.app.Activity
import android.app.Application
import kotlinx.android.synthetic.main.fragment_announcement_detailed.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import ru.labore.moderngymnasium.data.AppRepository
import ru.labore.moderngymnasium.data.db.entities.CommentEntity
import ru.labore.moderngymnasium.ui.adapters.DetailedAnnouncementRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.adapters.DetailedCommentRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.AuthoredEntityViewModel

class DetailedCommentViewModel(
    app: Application
) : AuthoredEntityViewModel(app) {
    fun getAdapter(
        currentFragment: DetailedCommentFragment
    ): DetailedCommentRecyclerViewAdapter {
        fragment = currentFragment

        val newAdapter = DetailedCommentRecyclerViewAdapter(this)
        newAdapter.updateAdditionalItems()

        adapter = newAdapter

        return newAdapter
    }

    suspend fun updateComments(
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

                val newComments = hashMapOf<Int, CommentEntity>()

                current = GlobalScope.async {
                    makeRequest(
                        activity,
                        {
                            getComments(offset, forceFetch).forEach {
                                newComments[it.id] = it
                            }
                        },
                        {
                            getComments(
                                offset,
                                AppRepository.Companion.UpdateParameters.DONT_UPDATE
                            ).forEach {
                                newComments[it.id] = it
                            }
                        }
                    )

                    loading = false
                }

                current?.join()

                if (refresh) {
                    val previousSize = itemCount

                    currentOffset = 0
                    reachedEnd = false
                    items.clear()
                    items.addAll(newComments.values.sortedByDescending {
                        it.createdAt
                    })

                    adapter.refreshItems(
                        previousSize,
                        itemCount
                    )
                } else {
                    if (newComments.isEmpty()) {
                        reachedEnd = true
                    } else {
                        val iterator = items.listIterator()

                        while (iterator.hasNext()) {
                            val it = iterator.next()
                            val newValue = newComments[it.id]

                            if (newValue != null) {
                                iterator.set(newValue)
                                newComments.remove(it.id)
                            }
                        }

                        val previousSize = itemCount

                        currentOffset += newComments.size
                        items.addAll(newComments.values)

                        adapter.pushItems(
                            previousSize,
                            itemCount
                        )
                    }
                }
            }
        } else {
            current?.join()
        }

        if (currentOffset > fragment.item.commentCount) {
            fragment.item.commentCount = currentOffset
            appRepository.persistFetchedAuthoredEntity(fragment.item)
        }

        loading = false
    }

    suspend fun setup(activity: Activity) {
        if (itemCount == 0)
            updateComments(
                activity,
                AppRepository.Companion.UpdateParameters.DETERMINE,
                true
            )
        else
            loading = false
    }

    private suspend fun getComments(
        offset: Int,
        forceFetch: AppRepository.Companion.UpdateParameters
    ) = appRepository.getComments(fragment.item.id, offset, forceFetch)
}