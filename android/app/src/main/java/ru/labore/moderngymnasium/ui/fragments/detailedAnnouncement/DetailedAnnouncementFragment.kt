package ru.labore.moderngymnasium.ui.fragments.detailedAnnouncement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.fragment_announcement_detailed.*
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.ui.base.ListElementFragment
import ru.labore.moderngymnasium.utils.announcementEntityToCaption

class DetailedAnnouncementFragment(
    controls: Companion.ListElementFragmentControls,
    private val announcement: AnnouncementEntity
) : ListElementFragment(controls) {
    override val viewModel: DetailedAnnouncementViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_announcement_detailed,
            container,
            false
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        announcementDetailedBackButton.setOnClickListener {
            controls.finish()
        }

        announcementDetailedAuthorName.text = if (announcement.author == null) {
            announcementDetailedAuthorRank.visibility = View.GONE
            resources.getString(R.string.no_author)
        } else {
            val caption = announcementEntityToCaption(
                announcement,
                resources.getString(R.string.author_no_name)
            )
            val comma = caption.indexOf(',')

            if (comma == -1) {
                announcementDetailedAuthorRank.visibility = View.GONE
                caption
            } else {
                announcementDetailedAuthorRank.text = caption.substring(comma + 2)
                caption.substring(0, comma)
            }
        }

        announcementDetailedText.text = announcement.text
    }
}