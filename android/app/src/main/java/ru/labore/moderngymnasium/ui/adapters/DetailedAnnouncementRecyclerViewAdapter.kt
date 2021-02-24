package ru.labore.moderngymnasium.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.ui.base.BaseRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.BaseViewHolder
import ru.labore.moderngymnasium.ui.fragments.detailedAnnouncement.DetailedAnnouncementViewModel
import ru.labore.moderngymnasium.utils.announcementEntityToCaption

class DetailedAnnouncementRecyclerViewAdapter(
    override val viewModel: DetailedAnnouncementViewModel
) : AuthoredEntityRecyclerViewAdapter(viewModel) {
    companion object DetailedAnnouncement {

        class AnnouncementViewHolder(private val layout: ConstraintLayout) :
            BaseViewHolder(layout) {

            override fun onBind(position: Int, parent: BaseRecyclerViewAdapter) {
                if (parent is AuthoredEntityRecyclerViewAdapter) {
                    val authorName = layout.getChildAt(1) as TextView
                    val authorRank = layout.getChildAt(2) as TextView
                    val text = layout.getChildAt(3) as TextView
                    val author = parent
                        .viewModel
                        .appRepository
                        .users[parent.viewModel.fragment.item.authorId]
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

                    text.text = parent.viewModel.fragment.item.text
                }
            }
        }

        const val ANNOUNCEMENT_VIEW_HOLDER_ID = "announcement"
    }

    public override fun updateAdditionalItems() {
        beginAdditionalItems.forEach {
            if (it.id == ANNOUNCEMENT_VIEW_HOLDER_ID)
                return
        }

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
                        ) as ConstraintLayout
                )
            }
        )

        super.updateAdditionalItems()
    }
}