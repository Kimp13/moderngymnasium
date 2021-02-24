package ru.labore.moderngymnasium.ui.fragments.detailedAnnouncement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_announcement_detailed.*
import kotlinx.android.synthetic.main.fragment_inbox.*
import kotlinx.coroutines.launch
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.AppRepository
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.data.db.entities.AuthoredEntity
import ru.labore.moderngymnasium.ui.base.AuthoredEntityFragment
import ru.labore.moderngymnasium.ui.base.BaseViewModel
import ru.labore.moderngymnasium.ui.base.ListElementFragment

class DetailedAnnouncementFragment(
    controls: Companion.ListElementFragmentControls,
    item: AuthoredEntity
) : AuthoredEntityFragment(controls, item) {
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

        launch {
            bindUI()
        }

        announcementDetailedBackButton.setOnClickListener {
            controls.finish()
        }

        announcementDetailedRecyclerView.apply {
            val viewManager = LinearLayoutManager(requireActivity())
            val divider = DividerItemDecoration(requireContext(), viewManager.orientation)

            divider.setDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.detailed_announcement_recycler_view_divider,
                    null
                )!!
            )

            addItemDecoration(divider)

            layoutManager = viewManager
            adapter = viewModel.getAdapter(this@DetailedAnnouncementFragment)

            scrollBy(0, savedInstanceState?.getInt("scrollY") ?: 0)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (height - scrollY <= 50)
                        launch {
                            viewModel.updateComments(
                                requireActivity()
                            )
                        }
                }
            })
        }
    }

    private suspend fun bindUI() {
        viewModel.setup(requireActivity())

        announcementDetailedRefresh.setOnRefreshListener {
            launch {
                viewModel.updateComments(
                    requireActivity(),
                    AppRepository.Companion.UpdateParameters.UPDATE,
                    true
                )

                announcementDetailedRefresh.isRefreshing = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(
            "scrollY",
            announcementDetailedRecyclerView.computeVerticalScrollOffset()
        )

        super.onSaveInstanceState(outState)
    }
}