package ru.labore.moderngymnasium.ui.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import java.util.*

class MainRecyclerViewAdapter(
    private val resources: Resources,
    private val announcements: Array<AnnouncementEntity>
) : RecyclerView.Adapter<MainRecyclerViewAdapter.MainViewHolder>() {
    class MainViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val linearLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.inbox_recycler_view, parent, false) as LinearLayout

        return MainViewHolder(linearLayout)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val authorView = (holder.layout.getChildAt(0) as TextView)
        val roleView = (holder.layout.getChildAt(1) as TextView)
        val classView = (holder.layout.getChildAt(2) as TextView)
        val textView = (holder.layout.getChildAt(3) as TextView)

        println(announcements[position].toString())

        if (announcements[position].author == null) {
            authorView.visibility = View.GONE
            roleView.visibility = View.GONE
            classView.visibility = View.GONE
        } else {
            val name: String = if (announcements[position].author!!.firstName != null) {
                if (announcements[position].author!!.lastName != null) {
                    announcements[position].author!!.firstName + " " +
                            announcements[position].author!!.lastName
                } else {
                    announcements[position].author!!.firstName!!
                }
            } else if (announcements[position].author!!.lastName != null) {
                announcements[position].author!!.lastName!!
            } else {
                resources.getString(R.string.author_no_name)
            }

            authorView.text = String.format(
                resources.getString(R.string.author_label),
                name
            )

            if (announcements[position].authorRole == null) {
                roleView.visibility = View.GONE
            } else {
                roleView.text = String.format(
                    resources.getString(R.string.role_label),
                    when(Locale.getDefault().language) {
                        "ru" -> announcements[position].authorRole!!.nameRu
                        else -> announcements[position].authorRole!!.name
                    }
                )
            }

            if (announcements[position].authorClass == null) {
                classView.visibility = View.GONE
            } else {
                classView.text = String.format(
                    resources.getString(R.string.class_label),
                    announcements[position].authorClass!!.name
                )
            }

            textView.text = String.format(
                resources.getString(R.string.text_label),
                announcements[position].text
            )
        }
    }

    override fun getItemCount() = announcements.size
}