package ru.labore.moderngymnasium.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.ui.base.BaseRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.BaseViewHolder
import ru.labore.moderngymnasium.ui.fragments.inbox.InboxViewModel
import ru.labore.moderngymnasium.utils.announcementEntityToCaption

class InboxRecyclerViewAdapter(
    override val viewModel: InboxViewModel
    ) : BaseRecyclerViewAdapter(viewModel) {
    companion object Inbox {
        private fun isWordCharacter(character: Char): Boolean =
            when (character.toInt()) {
                in 0..47 -> false
                in 58..64 -> false
                in 91..96 -> false
                in 123..126 -> false
                else -> true
            }

        private fun trimText(text: String): String {
            if (text.length <= shortTextCharCount)
                return text

            var i = shortTextCharCount

            while (isWordCharacter(text[--i]))
                if (i == 0)
                    return "${text.substring(0, shortTextCharCount)}…"

            val indexAfterFirstLoop = i

            while (isWordCharacter(text[--i]))
                if (i == 0)
                    return "${text.substring(indexAfterFirstLoop + 1)}…"

            return "${text.substring(0, i + 1)}…"
        }

        private const val shortTextCharCount = 200

        class AnnouncementViewHolder(private val layout: LinearLayout) :
            BaseViewHolder(layout) {
            override fun onBind(position: Int, parent: BaseRecyclerViewAdapter) {
                if (parent is InboxRecyclerViewAdapter) {
                    val constraintLayout = layout.getChildAt(0) as ConstraintLayout
                    val authorView = constraintLayout.getChildAt(1) as TextView
                    val authorRankView = constraintLayout.getChildAt(2) as TextView
                    val textView = layout.getChildAt(1) as TextView
                    val expandButton = layout.getChildAt(2)
                    val commentButton = layout.children.last() as TextView
                    val announcement = parent.viewModel.items[
                            position - parent.beginAdditionalItems.size
                    ] as AnnouncementEntity
                    val author = parent.viewModel.appRepository.users[
                            announcement.authorId
                    ]
                    val role = parent.viewModel.appRepository.roles[author?.roleId]
                    val `class` = parent.viewModel.appRepository.classes[author?.classId]

                    layout.setOnClickListener {
                        parent.viewModel.onAnnouncementClick(
                            announcement
                        )
                    }

                    authorView.text = if (author == null) {
                        authorRankView.visibility = View.GONE
                        parent.viewModel.app.resources.getString(R.string.no_author)
                    } else {
                        val caption = announcementEntityToCaption(
                            author,
                            parent.viewModel.app.resources.getString(R.string.noname),
                            role,
                            `class`,
                        )
                        val comma = caption.indexOf(',')

                        if (comma == -1) {
                            authorRankView.visibility = View.GONE
                            caption
                        } else {
                            authorRankView.text = caption.substring(comma + 2)
                            caption.substring(0, comma)
                        }
                    }

                    if (announcement.text.length <= shortTextCharCount) {
                        textView.text = announcement.text
                        expandButton.visibility = View.GONE
                    } else {
                        textView.text = trimText(announcement.text)
                        expandButton.visibility = View.VISIBLE
                        expandButton.setOnClickListener {
                            textView.text = announcement.text
                            it.visibility = View.GONE
                        }
                    }

                    val count = announcement.commentCount

                    if (count > 0)
                        commentButton.text = count.toString()
                }
            }
        }

        class CreateViewHolder(private val layout: ConstraintLayout) :
            BaseViewHolder(layout) {
            override fun onBind(position: Int, parent: BaseRecyclerViewAdapter) {
                if (parent is InboxRecyclerViewAdapter) {
                    val button = layout.getChildAt(0) as Button?

                    button?.setOnClickListener {
                        parent.viewModel.onCreateButtonClick()
                    }
                }
            }
        }

        const val CREATE_VIEW_HOLDER_ID = "create"
    }

    override fun createDefaultViewHolder(
        parent: ViewGroup
    ) = AnnouncementViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(
                R.layout.inbox_view_holder,
                parent,
                false
            ) as LinearLayout
    )

    public override fun updateAdditionalItems() {
        super.updateAdditionalItems()

        if (viewModel.appRepository
                .user
                ?.data
                ?.permissions
                ?.get("announcement")
                ?.get("create")
                ?.isNotEmpty() == true
        ) {
            var absent = true
            val size = beginAdditionalItems.size

            for (i in 0 until size) {
                if (beginAdditionalItems[i].id == CREATE_VIEW_HOLDER_ID) {
                    absent = false
                    break
                }
            }

            if (absent) {
                beginAdditionalItems.add(
                    Base.AdditionalItem(
                        CREATE_VIEW_HOLDER_ID
                    )
                    { parent ->
                        CreateViewHolder(
                            LayoutInflater.from(parent.context)
                                .inflate(
                                    R.layout.inbox_create_view_holder,
                                    parent,
                                    false
                                ) as ConstraintLayout
                        )
                    }
                )

                notifyItemInserted(size)
            }
        } else {
            for (i in 0 until beginAdditionalItems.size) {
                if (beginAdditionalItems[i].id == CREATE_VIEW_HOLDER_ID) {
                    beginAdditionalItems.removeAt(i)
                    notifyItemRemoved(i)
                    break
                }
            }
        }
    }

    override val defaultItemCount: Int
        get() = viewModel.items.size
}