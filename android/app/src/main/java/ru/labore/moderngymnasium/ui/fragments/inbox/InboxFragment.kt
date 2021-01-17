package ru.labore.moderngymnasium.ui.fragments.inbox

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_inbox.*
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.network.ClientConnectionException
import ru.labore.moderngymnasium.data.network.ClientErrorException
import ru.labore.moderngymnasium.data.repository.AnnouncementsWithCount
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.activities.AnnouncementDetailedActivity
import ru.labore.moderngymnasium.ui.activities.LoginActivity
import ru.labore.moderngymnasium.ui.activities.MainActivity
import ru.labore.moderngymnasium.ui.adapters.MainRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.ListElementFragment
import ru.labore.moderngymnasium.ui.base.ScopedFragment
import java.net.ConnectException
import kotlin.properties.Delegates

class InboxFragment(push: (Fragment) -> Unit, finish: () -> Unit) : ListElementFragment(
    push,
    finish
), DIAware {
    override val di: DI by lazy { (context as DIAware).di }

    private val viewModel: InboxViewModel by viewModels()

    private var loading = true
    private var overallCount by Delegates.notNull<Int>()
    private var currentCount = 0
    private lateinit var viewAdapter: MainRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inbox, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        bindUI(savedInstanceState)

        viewModel.appRepository.unreadAnnouncementsPushListener = {
            viewAdapter.prependAnnouncement(it)

            requireActivity().let { activity ->
                if (activity is MainActivity) {
                    activity.updateInboxBadge(viewModel.appRepository.unreadAnnouncements.size)
                }
            }
        }

        viewAdapter = MainRecyclerViewAdapter(
            resources,
            mutableListOf()
        ) {
            val intent = Intent(requireContext(), AnnouncementDetailedActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("announcement", it)
            intent.putExtras(bundle)
            startActivity(intent)
        }

        inboxRecyclerView.apply {
            val viewManager = LinearLayoutManager(requireActivity())
            val divider = DividerItemDecoration(requireContext(), viewManager.orientation)
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.inbox_recycler_view_divider,
                null
            )?.let {
                divider.setDrawable(it)
            }

            addItemDecoration(divider)
            setHasFixedSize(true)

            layoutManager = viewManager
            adapter = viewAdapter

            scrollBy(0, savedInstanceState?.getInt("scrollY") ?: 0)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (layoutManager is LinearLayoutManager) {
                        val firstItemIndex = (layoutManager as LinearLayoutManager)
                            .findFirstCompletelyVisibleItemPosition()

                        viewModel.appRepository.unreadAnnouncements.let { list ->
                            if (firstItemIndex < list.size) {
                                for (i in list.size - 1 downTo firstItemIndex) {
                                    list.removeAt(i)
                                }

                                requireActivity().let {
                                    if (it is MainActivity) {
                                        it.updateInboxBadge(list.size)
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (
                        !recyclerView.canScrollVertically(1) &&
                        currentCount < overallCount
                    ) {
                        addNewAnnouncements()
                    }

                    if (recyclerView.canScrollVertically(-1)) {
                        inboxAnnounceButton.shrink()
                    } else {
                        inboxAnnounceButton.extend()
                    }
                }
            })
        }
    }

    private suspend fun getAnnouncements(
        offset: Int = currentCount,
        forceFetch: Boolean = false
    ): AnnouncementsWithCount =
        try {
            viewModel.getAnnouncements(offset, forceFetch)
        } catch(e: Exception) {
            val activity = requireActivity()

            Toast.makeText(
                activity,
                when (e) {
                    is ConnectException -> getString(R.string.server_unavailable)
                    is ClientConnectionException -> getString(R.string.no_internet)
                    is ClientErrorException -> {
                        if (e.errorCode == AppRepository.HTTP_RESPONSE_CODE_UNAUTHORIZED) {
                            viewModel.appRepository.user = null
                            startActivity(Intent(activity, LoginActivity::class.java))
                            activity.finish()
                        }

                        getString(R.string.session_timed_out)
                    }
                    else -> "An unknown error occurred."
                },
                Toast.LENGTH_SHORT
            ).show()

            viewModel.getAnnouncements(offset, null)
        }

    private fun addNewAnnouncements() = launch {
        if (!loading) {
            loading = true
            inboxProgressBar.visibility = View.VISIBLE

            val newAnnouncements = getAnnouncements()

            currentCount += newAnnouncements.currentCount

            viewAdapter.pushAnnouncements(newAnnouncements.data)

            loading = false
            inboxProgressBar.visibility = View.GONE
        }
    }

    private fun refreshUI() = launch {
        val announcements = getAnnouncements(0, true)

        currentCount = announcements.currentCount
        overallCount = announcements.overallCount

        viewAdapter.refreshAnnouncements(announcements.data)

        inboxRefreshLayout.isRefreshing = false
    }

    private fun bindUI(savedInstanceState: Bundle?) = launch {
        val announcements = savedInstanceState?.getParcelable("announcements") ?:
            getAnnouncements()

        val params =
            inboxProgressBar.layoutParams as ConstraintLayout.LayoutParams

        loading = false

        overallCount = announcements.overallCount
        currentCount = announcements.currentCount

        inboxProgressBar.visibility = View.GONE
        inboxProgressBarCaption.visibility = View.GONE

        params.topToTop = ConstraintLayout.LayoutParams.UNSET
        params.bottomToBottom = R.id.inboxFragmentLayout
        params.bottomMargin = 50

        viewAdapter.pushAnnouncements(announcements.data)

        inboxRefreshLayout.setOnRefreshListener {
            refreshUI()
        }

        inboxAnnounceButton.setOnClickListener {
            val location = IntArray(2)

            inboxAnnounceButton.getLocationInWindow(location)

            (requireActivity() as MainActivity).revealCreateFragment(
                location[0] + it.width / 2,
                location[1] - it.height / 2,
                it.height.toFloat()
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("scrollY", inboxRecyclerView.computeVerticalScrollOffset())
        outState.putParcelable("announcements", AnnouncementsWithCount(
            overallCount,
            currentCount,
            viewAdapter.announcements.toTypedArray()
        ))

        println("Saving!!!")
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()

        viewModel.appRepository.unreadAnnouncementsPushListener = null
    }
}