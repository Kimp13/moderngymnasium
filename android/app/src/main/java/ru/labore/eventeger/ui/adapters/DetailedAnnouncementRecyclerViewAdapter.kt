package ru.labore.eventeger.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.children
import ru.labore.eventeger.R
import ru.labore.eventeger.data.db.entities.AnnouncementEntity
import ru.labore.eventeger.ui.base.DetailedAuthoredEntityRecyclerViewAdapter
import ru.labore.eventeger.ui.base.BaseRecyclerViewAdapter
import ru.labore.eventeger.ui.base.BaseViewHolder
import ru.labore.eventeger.ui.fragments.detailedAnnouncement.DetailedAnnouncementViewModel
import ru.labore.eventeger.utils.announcementEntityToCaption

class DetailedAnnouncementRecyclerViewAdapter(
    override val viewModel: DetailedAnnouncementViewModel
) : DetailedAuthoredEntityRecyclerViewAdapter(viewModel) {
    companion object DetailedAnnouncement {

        open class AnnouncementViewHolder(protected val layout: RelativeLayout) :
            BaseViewHolder(layout) {

            override fun onBind(
                position: Int,
                parent: BaseRecyclerViewAdapter
            ) {
                val iterator = layout.children.iterator().apply { next() }

                val authorName = iterator.next() as TextView
                val time = iterator.next() as TextView
                val authorRank = iterator.next() as TextView
                val text = iterator.next() as TextView
                val start = iterator.next() as TextView
                val end = iterator.next() as TextView

                val item = if (parent is DetailedAnnouncementRecyclerViewAdapter) {
                    parent.viewModel.item
                } else {
                    parent.viewModel.items[position - parent.beginAdditionalItems.size]
                } as AnnouncementEntity

                val author = parent
                    .viewModel
                    .appRepository
                    .users[item.authorId]
                val role = parent.viewModel.appRepository.roles[author?.roleId]
                val `class` = parent.viewModel.appRepository.classes[author?.classId]

                authorName.text = if (author == null) {
                    authorRank.visibility = View.GONE
                    parent.viewModel.app.resources.getString(R.string.no_author)
                } else {
                    val caption = announcementEntityToCaption(
                        author,
                        parent.viewModel.app.resources.getString(R.string.noname),
                        role,
                        `class`
                    )
                    val comma = caption.indexOf(',')

                    if (comma == -1) {
                        authorRank.visibility = View.GONE
                        caption
                    } else {
                        authorRank.text = caption.substring(comma + 2)
                        caption.substring(0, comma)
                    }
                }

                time.text = DateUtils.getRelativeTimeSpanString(
                    item.createdAt.toEpochSecond() * 1000,
                    parent.viewModel.appRepository.now().toEpochSecond() * 1000,
                    0
                )

                text.text = item.text

                if (item.isEvent) {
                    start.visibility = View.VISIBLE
                    end.visibility = View.VISIBLE
                    val now = parent.viewModel.appRepository.now().toEpochSecond() * 1000

                    start.text = if (item.startsAt == null) {
                        parent.viewModel.app.getString(R.string.ongoing)
                    } else {
                        if (now > item.startsAt!!.toEpochSecond() * 1000) {
                            parent.viewModel.app.getString(
                                R.string.started_at_fmt,
                                DateUtils.getRelativeTimeSpanString(
                                    item.startsAt!!.toEpochSecond() * 1000,
                                    now,
                                    0
                                )
                            )
                        } else {
                            parent.viewModel.app.getString(
                                R.string.starts_at_fmt,
                                DateUtils.getRelativeTimeSpanString(
                                    item.startsAt!!.toEpochSecond() * 1000,
                                    now,
                                    0
                                )
                            )
                        }
                    }

                    end.text = if (item.endsAt == null) {
                        parent.viewModel.app.getString(R.string.never_ends)
                    } else {
                        if (now > item.endsAt!!.toEpochSecond() * 1000) {
                            parent.viewModel.app.getString(
                                R.string.ended_at_fmt,
                                DateUtils.getRelativeTimeSpanString(
                                    item.endsAt!!.toEpochSecond() * 1000,
                                    now,
                                    0
                                )
                            )
                        } else {
                            parent.viewModel.app.getString(
                                R.string.ends_at_fmt,
                                DateUtils.getRelativeTimeSpanString(
                                    item.endsAt!!.toEpochSecond() * 1000,
                                    now,
                                    0
                                )
                            )
                        }
                    }
                } else {
                    start.visibility = View.GONE
                    end.visibility = View.GONE
                }
            }
        }

        const val ANNOUNCEMENT_VIEW_HOLDER_ID = "announcement"
    }

    public override fun updateAdditionalItems() {
        var absent = true

        for (i in 0 until beginAdditionalItems.size) {
            if (beginAdditionalItems[i].id == ANNOUNCEMENT_VIEW_HOLDER_ID) {
                absent = false
                break
            }
        }

        if (absent)
            beginAdditionalItems.add(
                0,
                Base.AdditionalItem(
                    ANNOUNCEMENT_VIEW_HOLDER_ID
                ) { parent ->
                    AnnouncementViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.detailed_announcement_view_holder,
                                parent,
                                false
                            ) as RelativeLayout
                    )
                }
            )

        super.updateAdditionalItems()
    }
}