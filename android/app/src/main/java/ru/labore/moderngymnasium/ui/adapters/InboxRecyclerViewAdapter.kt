package ru.labore.moderngymnasium.ui.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    var announcements: MutableList<AnnouncementEntity>,
    private val clickHandler: (AnnouncementEntity) -> Unit = {}
) : RecyclerView.Adapter<InboxRecyclerViewAdapter.MainViewHolder>() {
    companion object {
        private fun isWordCharacter(character: Char): Boolean = when(character.toInt()) {
            in 0..47 -> false
            in 58..64 -> false
            in 91..96 -> false
            in 123..126 -> false
            else -> true
        }

        private fun trimTextTo(text: String, amount: Int): String {
            if (text.length <= amount) {
                return text
            }

            var i = amount

            while (isWordCharacter(text[--i])) {
                if (i == 0) {
                    return "${text.substring(0, amount)}…"
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
    }

    class MainViewHolder(val card: MaterialCardView) : RecyclerView.ViewHolder(card)
    var isClickable: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val card = LayoutInflater.from(parent.context)
            .inflate(R.layout.inbox_recycler_view, parent, false) as MaterialCardView

        return MainViewHolder(card)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val linearLayout = holder.card.getChildAt(0) as LinearLayout
        val constraintLayout = linearLayout.getChildAt(0) as ConstraintLayout
        val authorView = constraintLayout.getChildAt(1) as TextView
        val authorRankView = constraintLayout.getChildAt(2) as TextView
        val textView = linearLayout.getChildAt(1) as TextView
        val expandButton = linearLayout.getChildAt(2)

        holder.card.setOnClickListener {
            if (isClickable) {
                clickHandler(announcements[position])
            }
        }

        authorView.text = if (announcements[position].author == null) {
            authorRankView.visibility = View.GONE
            resources.getString(R.string.no_author)
        } else {
            val caption = announcementEntityToCaption(
                announcements[position],
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

        if (announcements[position].text.length <= 200) {
            textView.text = announcements[position].text
            expandButton.visibility = View.GONE
        } else {
            textView.text = trimTextTo(announcements[position].text, 200)
            expandButton.visibility = View.VISIBLE
            expandButton.setOnClickListener {
                textView.text = announcements[position].text
                it.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = announcements.size

    fun prependAnnouncement(
        announcement: AnnouncementEntity
    ) {
        announcements.add(0, announcement)

        notifyItemInserted(0)
    }

    fun refreshAnnouncements(
        newAnnouncements: Array<AnnouncementEntity>
    ) {
        val count = itemCount

        announcements = mutableListOf()

        notifyItemRangeRemoved(0, count)

        newAnnouncements.forEach {
            announcements.add(it)
        }

        notifyItemRangeInserted(0, announcements.size)
    }

    fun pushAnnouncements(
        newAnnouncements: Array<AnnouncementEntity>
    ) {
        val positionStart = itemCount

        announcements.addAll(newAnnouncements)

        println(announcements.toString())

        notifyItemRangeInserted(
            positionStart,
            newAnnouncements.size
        )
    }
}