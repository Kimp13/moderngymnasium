package ru.labore.moderngymnasium.ui.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.AppRepository
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.ui.base.BaseRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.BaseViewHolder
import ru.labore.moderngymnasium.utils.announcementEntityToCaption
import kotlin.math.min

class InboxRecyclerViewAdapter(
    private val resources: Resources,
    private val appRepository: AppRepository,
    private val announcements: MutableList<AnnouncementEntity>,
    private val createClickHandler: () -> Unit,
    private val announcementClickHandler: (AnnouncementEntity) -> Unit = {}
) : BaseRecyclerViewAdapter() {
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
            if (text.length <= shortTextCharCount) {
                return text
            }

            var i = shortTextCharCount

            while (isWordCharacter(text[--i])) {
                if (i == 0) {
                    return "${text.substring(0, shortTextCharCount)}…"
                }
            }

            val indexAfterFirstLoop = i
            while (isWordCharacter(text[--i])) {
                if (i == 0) {
                    return "${text.substring(indexAfterFirstLoop + 1)}…"
                }
            }

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
                    val pos = position - parent.beginAdditionalItems.size
                    val author = parent.appRepository.users[
                            parent.announcements[pos].authorId
                    ]
                    val role = parent.appRepository.roles[author?.roleId]
                    val `class` = parent.appRepository.classes[author?.classId]

                    layout.setOnClickListener {
                        parent.announcementClickHandler(
                            parent.announcements[pos]
                        )
                    }

                    authorView.text = if (author == null) {
                        authorRankView.visibility = View.GONE
                        parent.resources.getString(R.string.no_author)
                    } else {
                        val caption = announcementEntityToCaption(
                            author,
                            parent.resources.getString(R.string.noname),
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

                    if (parent.announcements[pos].text.length <= shortTextCharCount) {
                        textView.text = parent.announcements[pos].text
                        expandButton.visibility = View.GONE
                    } else {
                        textView.text = trimText(parent.announcements[pos].text)
                        expandButton.visibility = View.VISIBLE
                        expandButton.setOnClickListener {
                            textView.text = parent.announcements[pos].text
                            it.visibility = View.GONE
                        }
                    }

                    val count = parent.announcements[pos].commentCount

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
                        parent.createClickHandler()
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
                R.layout.inbox_view_handler,
                parent,
                false
            ) as LinearLayout
    )

    override fun updateAdditionalItems() {
        if (
            appRepository
                .user
                ?.data
                ?.permissions
                ?.get("announcement")
                ?.get("create")
                ?.isNotEmpty() == true
        ) {
            if (beginAdditionalItems.isEmpty())
                beginAdditionalItems.add(
                    Base.AdditionalItem(
                        CREATE_VIEW_HOLDER_ID
                    )
                    { parent ->
                        CreateViewHolder(
                            LayoutInflater.from(parent.context)
                                .inflate(
                                    R.layout.inbox_create_view,
                                    parent,
                                    false
                                ) as ConstraintLayout
                        )
                    }
                )
        } else if (beginAdditionalItems.size == 1) {
            beginAdditionalItems.clear()
        }
    }

    override val defaultItemCount: Int
        get() = announcements.size

    fun prependAnnouncement() {
        notifyItemInserted(beginAdditionalItems.size)
    }

    fun refreshAnnouncements(
        newSize: Int,
        previousSize: Int
    ) {
        if (previousSize > newSize) {
            notifyItemRangeRemoved(
                beginAdditionalItems.size + newSize,
                previousSize - newSize
            )
        }

        notifyItemRangeChanged(
            beginAdditionalItems.size,
            min(newSize, previousSize)
        )

        if (previousSize < newSize) {
            notifyItemRangeInserted(
                itemCount,
                newSize - previousSize
            )
        }
    }

    fun pushAnnouncements(
        previousSize: Int,
        addedSize: Int
    ) {
        notifyItemRangeInserted(
            previousSize + beginAdditionalItems.size,
            addedSize
        )
    }
}