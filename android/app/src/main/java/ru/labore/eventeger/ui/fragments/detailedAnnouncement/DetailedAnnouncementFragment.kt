package ru.labore.eventeger.ui.fragments.detailedAnnouncement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_announcement_detailed.*
import ru.labore.eventeger.R
import ru.labore.eventeger.data.AppRepository
import ru.labore.eventeger.data.db.entities.AuthoredEntity
import ru.labore.eventeger.ui.base.DetailedAuthoredEntityFragment
import ru.labore.eventeger.ui.base.ITEM_KEY

class DetailedAnnouncementFragment : DetailedAuthoredEntityFragment() {
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

        launch {
            bindUI()
        }
    }

    override fun onItemClicked(item: AuthoredEntity) {
        findNavController().navigate(
            R.id.action_announcement_detailed_to_comment,
            bundleOf(ITEM_KEY to item)
        )
    }

    private fun bindUI() {
        launch {
            viewModel.setup(requireActivity())
        }

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

    override fun onPause() {
        super.onPause()

        announcementDetailedRefresh.setOnRefreshListener(null)
    }

    override fun onResume() {
        super.onResume()

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
}