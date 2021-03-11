package ru.labore.eventeger.ui.fragments.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_inbox.*
import ru.labore.eventeger.R
import ru.labore.eventeger.data.AppRepository
import ru.labore.eventeger.ui.activities.MainActivity
import ru.labore.eventeger.ui.base.BaseFragment
import ru.labore.eventeger.ui.base.ITEM_KEY

class InboxFragment : BaseFragment() {
    override val viewModel: InboxViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inbox, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        inboxRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireActivity())

            adapter = viewModel.getAdapter({
                findNavController().navigate(
                    R.id.action_announcements_to_detailed,
                    bundleOf(ITEM_KEY to it)
                )
            }, {
                findNavController().navigate(R.id.action_announcements_to_create)
            })

            addItemDecoration(DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            ).apply {
                setDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.inbox_recycler_view_divider,
                        null
                    )!!
                )
            })
            setHasFixedSize(true)

            scrollBy(0, savedInstanceState?.getInt("scrollY") ?: 0)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (height - scrollY <= 50)
                        launch {
                            viewModel.updateAnnouncements(
                                requireActivity()
                            )
                        }

                    if (layoutManager is LinearLayoutManager) {
                        val firstItemIndex = (layoutManager as LinearLayoutManager)
                            .findFirstCompletelyVisibleItemPosition()

                        viewModel.appRepository.unreadAnnouncements.let { list ->
                            if (firstItemIndex < list.size) {
                                for (i in list.size - 1 downTo firstItemIndex)
                                    list.removeAt(i)

                                requireActivity().let {
                                    if (it is MainActivity)
                                        it.updateInboxBadge(list.size)
                                }
                            }
                        }
                    }
                }
            })
        }

        bindUI()
    }

    private fun bindUI() = launch {
        viewModel.setup(requireActivity())

        inboxRefreshLayout.setOnRefreshListener {
            launch {
                viewModel.updateAnnouncements(
                    requireActivity(),
                    AppRepository.Companion.UpdateParameters.UPDATE,
                    true
                )

                inboxRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("scrollY", inboxRecyclerView.computeVerticalScrollOffset())

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        viewModel.appRepository.unreadAnnouncementsPushListener = null

        super.onDestroy()
    }
}