package ru.labore.moderngymnasium.ui.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.utils.announcementEntityToCaption
import java.lang.reflect.Constructor

class MainRecyclerViewAdapter(
    private val resources: Resources,
    private var announcements: MutableList<AnnouncementEntity>
) : RecyclerView.Adapter<MainRecyclerViewAdapter.MainViewHolder>() {
    class MainViewHolder(val card: MaterialCardView) : RecyclerView.ViewHolder(card)

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

        textView.text = announcements[position].text
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

        notifyItemRangeInserted(
            positionStart,
            newAnnouncements.size
        )
    }
}