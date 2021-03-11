package ru.labore.eventeger.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.children
import ru.labore.eventeger.R
import ru.labore.eventeger.data.db.entities.AnnouncementEntity
import ru.labore.eventeger.ui.base.BaseRecyclerViewAdapter
import ru.labore.eventeger.ui.base.BaseViewHolder
import ru.labore.eventeger.ui.fragments.inbox.InboxViewModel
import ru.labore.eventeger.utils.announcementEntityToCaption

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

        class AnnouncementViewHolder(layout: RelativeLayout) :
            DetailedAnnouncementRecyclerViewAdapter
            .DetailedAnnouncement
            .AnnouncementViewHolder(layout) {
            override fun onBind(position: Int, parent: BaseRecyclerViewAdapter) {
                super.onBind(position, parent)

                if (parent is InboxRecyclerViewAdapter) {
                    val textView = layout.getChildAt(4) as TextView
                    val expandButton = layout.getChildAt(7) as TextView
                    val commentButton = layout.getChildAt(8) as TextView

                    val announcement = parent.viewModel.items[
                            position - parent.beginAdditionalItems.size
                    ] as AnnouncementEntity

                    layout.setOnClickListener {
                        parent.viewModel.onAnnouncementClick(
                            announcement
                        )
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

                    val count = announcement.commentsCount

                    if (count > 0) {
                        commentButton.visibility = View.VISIBLE
                        commentButton.text = count.toString()
                    } else {
                        commentButton.visibility = View.GONE
                    }
                }
            }
        }

        class CreateViewHolder(private val layout: FrameLayout) :
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
            ) as RelativeLayout
    )

    public override fun updateAdditionalItems() {
        super.updateAdditionalItems()

        if (
            viewModel.appRepository.appNetwork.isOnline() &&
            viewModel.appRepository
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
                                ) as FrameLayout
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