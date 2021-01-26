package ru.labore.moderngymnasium.ui.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.utils.announcementEntityToCaption

class InboxRecyclerViewAdapter(
    private val resources: Resources,
    private val createClickHandler: () -> Unit,
    private val announcementClickHandler: (AnnouncementEntity) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private fun isWordCharacter(character: Char): Boolean = when(character.toInt()) {
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
            while(isWordCharacter(text[--i])) {
                if (i == 0) {
                    return "${text.substring(indexAfterFirstLoop + 1)}…"
                }
            }

            return "${text.substring(0, i + 1)}…"
        }

        const val additionalItems = 1
        private const val shortTextCharCount = 200

        const val CREATE_VIEW_POSITION = 0
        const val ANNOUNCEMENT_VIEW_POSITION = -1
    }

    class AnnouncementViewHolder(val card: MaterialCardView) :
        RecyclerView.ViewHolder(card)

    class CreateViewHolder(val layout: ConstraintLayout) :
        RecyclerView.ViewHolder(layout)

    val announcements = mutableListOf<AnnouncementEntity>()

    override fun getItemViewType(position: Int): Int =
        when (position) {
            CREATE_VIEW_POSITION -> CREATE_VIEW_POSITION
            else -> ANNOUNCEMENT_VIEW_POSITION
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder = when(viewType) {
        CREATE_VIEW_POSITION -> CreateViewHolder(
                LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.inbox_create_view,
                    parent,
                    false
                ) as ConstraintLayout
            )
        else -> AnnouncementViewHolder(
                LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.inbox_recycler_view,
                    parent,
                    false
                ) as MaterialCardView
            )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CreateViewHolder) {
            val button = holder.layout.getChildAt(0) as Button

            button.setOnClickListener {
                createClickHandler()
            }
        } else if (holder is AnnouncementViewHolder) {
            val linearLayout = holder.card.getChildAt(0) as LinearLayout
            val constraintLayout = linearLayout.getChildAt(0) as ConstraintLayout
            val authorView = constraintLayout.getChildAt(1) as TextView
            val authorRankView = constraintLayout.getChildAt(2) as TextView
            val textView = linearLayout.getChildAt(1) as TextView
            val expandButton = linearLayout.getChildAt(2)
            val pos = position - additionalItems

            holder.card.setOnClickListener {
                announcementClickHandler(announcements[pos])
            }

            authorView.text = if (announcements[pos].author == null) {
                authorRankView.visibility = View.GONE
                resources.getString(R.string.no_author)
            } else {
                val caption = announcementEntityToCaption(
                    announcements[pos],
                    resources.getString(R.string.author_no_name)
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

            if (announcements[pos].text.length <= shortTextCharCount) {
                textView.text = announcements[pos].text
                expandButton.visibility = View.GONE
            } else {
                textView.text = trimText(announcements[pos].text)
                expandButton.visibility = View.VISIBLE
                expandButton.setOnClickListener {
                    textView.text = announcements[pos].text
                    it.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemCount() = announcements.size + additionalItems

    fun prependAnnouncement(
        announcement: AnnouncementEntity
    ) {
        announcements.add(0, announcement)

        notifyItemInserted(additionalItems)
    }

    fun refreshAnnouncements(
        newAnnouncements: Array<AnnouncementEntity>
    ) {
        val count = itemCount

        announcements.clear()

        notifyItemRangeRemoved(
            additionalItems,
            count + additionalItems
        )

        newAnnouncements.forEach {
            announcements.add(it)
        }

        notifyItemRangeInserted(
            additionalItems,
            announcements.size + additionalItems
        )
    }

    fun pushAnnouncements(
        newAnnouncements: Array<AnnouncementEntity>
    ) {
        val positionStart = itemCount

        announcements.addAll(newAnnouncements)

        notifyItemRangeInserted(
            positionStart + additionalItems,
            newAnnouncements.size + additionalItems
        )
    }
}