package ru.labore.moderngymnasium.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.entities.CommentEntity
import ru.labore.moderngymnasium.ui.base.BaseRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.BaseViewHolder
import ru.labore.moderngymnasium.ui.fragments.detailedComment.DetailedCommentViewModel
import ru.labore.moderngymnasium.utils.announcementEntityToCaption

class DetailedCommentRecyclerViewAdapter(
    override val viewModel: DetailedCommentViewModel
) : AuthoredEntityRecyclerViewAdapter(viewModel) {
    companion object DetailedAnnouncement {

        class ParentCommentViewHolder(private val layout: ConstraintLayout) :
            BaseViewHolder(layout) {
            override fun onBind(position: Int, parent: BaseRecyclerViewAdapter) {
                if (parent is DetailedCommentRecyclerViewAdapter) {
                    val headline = layout.getChildAt(1) as TextView
                    val time = layout.getChildAt(2) as TextView
                    val text = layout.getChildAt(3) as TextView
                    val comment = parent.viewModel.fragment.item as CommentEntity
                    val author = parent.viewModel.appRepository.users[comment.authorId]
                    val iconButton = layout.children.last() as TextView

                    headline.text = if (author == null)
                        parent.viewModel.app.resources.getString(R.string.no_author)
                    else if (
                        author.firstName != null &&
                        author.lastName != null
                    )
                        "${author.firstName} ${author.lastName}"
                    else author.firstName
                        ?: parent.viewModel.app.resources.getString(R.string.noname)

                    time.text = DateUtils.getRelativeTimeSpanString(
                        comment.createdAt.toEpochSecond() * 1000,
                        parent.viewModel.appRepository.now().toEpochSecond() * 1000,
                        0
                    )

                    text.text = comment.text

                    if (comment.childrenCount > 0) {
                        iconButton.visibility = View.VISIBLE
                        iconButton.text = comment.childrenCount.toString()
                    }
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
                ParentCommentViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(
                            R.layout.detailed_comment_view_holder,
                            parent,
                            false
                        ) as ConstraintLayout
                )
            }
        )

        super.updateAdditionalItems()
    }
}