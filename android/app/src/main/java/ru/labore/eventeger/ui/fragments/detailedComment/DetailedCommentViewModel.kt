package ru.labore.eventeger.ui.fragments.detailedComment

import android.app.Activity
import android.app.Application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.labore.eventeger.data.AppRepository
import ru.labore.eventeger.data.db.entities.CommentEntity
import ru.labore.eventeger.ui.adapters.DetailedCommentRecyclerViewAdapter
import ru.labore.eventeger.ui.base.DetailedAuthoredEntityViewModel

class DetailedCommentViewModel(app: Application) : DetailedAuthoredEntityViewModel(app) {
    fun getAdapter(newFragment: DetailedCommentFragment) =
        DetailedCommentRecyclerViewAdapter(this).apply {
            (newFragment.item as CommentEntity).let {
                replyTo = it.id
                announcementId = it.announcementId
            }

            fragment = newFragment
            adapter = this

            updateAdditionalItems()
        }

    suspend fun updateComments(
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

                val newComments = hashMapOf<Int, CommentEntity>()

                current = GlobalScope.launch {
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
                }

                current?.join()

                if (refresh) {
                    val previousSize = itemCount

                    reachedEnd = false
                    items.clear()
                    items.addAll(newComments.values.sortedByDescending {
                        it.createdAt
                    })
                    currentOffset = items.size

                    refreshItems(
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
                        items.addAll(newComments.values.sortedByDescending {
                            it.createdAt
                        })

                        pushItems(
                            previousSize,
                            itemCount
                        )
                    }
                }
            }
        } else {
            current?.join()
        }

        if (currentOffset > fragment.item.commentsCount) {
            fragment.item.commentsCount = currentOffset
            appRepository.persistFetchedAuthoredEntity(fragment.item)
        }
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
    ) = appRepository.getComments(announcementId, offset, forceFetch, replyTo)
}