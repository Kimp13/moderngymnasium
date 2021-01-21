package ru.labore.moderngymnasium.ui.fragments.inbox

import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
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
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.activities.AnnouncementDetailedActivity
import ru.labore.moderngymnasium.ui.activities.LoginActivity
import ru.labore.moderngymnasium.ui.activities.MainActivity
import ru.labore.moderngymnasium.ui.adapters.InboxRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.ListElementFragment
import java.net.ConnectException
import kotlin.properties.Delegates

class InboxFragment(push: (Fragment) -> Unit, finish: () -> Unit) : ListElementFragment(
    push,
    finish
), DIAware {
    override val di: DI by lazy { (context as DIAware).di }

    private val viewModel: InboxViewModel by viewModels()

    private var loading = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inbox, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        bindUI(savedInstanceState)

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

            adapter = viewModel.bindAdapter()

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
                        !recyclerView.canScrollVertically(1)
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

    private suspend fun updateAnnouncements(
        forceFetch: AppRepository.Companion.UpdateParameters =
            AppRepository.Companion.UpdateParameters.DETERMINE,
        refresh: Boolean = false
    ) =
        try {
            viewModel.updateAnnouncements(forceFetch, refresh)
        } catch(e: Exception) {
            println(e.toString())
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

                            getString(R.string.session_timed_out)
                        } else {
                            "An unknown error has occurred."
                        }
                    }
                    else -> "An unknown error has occurred."
                },
                Toast.LENGTH_SHORT
            ).show()

            viewModel.updateAnnouncements(
                AppRepository.Companion.UpdateParameters.DONT_UPDATE,
                refresh
            )
        }

    private fun addNewAnnouncements() = launch {
        if (!loading) {
            loading = true
            inboxProgressBar.visibility = View.VISIBLE

            updateAnnouncements()

            loading = false
            inboxProgressBar.visibility = View.GONE
        }
    }

    private fun refreshUI() = launch {
        updateAnnouncements(AppRepository.Companion.UpdateParameters.UPDATE, true)

        inboxRefreshLayout.isRefreshing = false
    }

    private fun bindUI(savedInstanceState: Bundle?) = launch {
        val params =
            inboxProgressBar.layoutParams as ConstraintLayout.LayoutParams

        inboxProgressBar.visibility = View.GONE
        inboxProgressBarCaption.visibility = View.GONE

        params.topToTop = ConstraintLayout.LayoutParams.UNSET
        params.bottomToBottom = R.id.inboxFragmentLayout
        params.bottomMargin = 50

        if (viewModel.itemCount == 0) {
            updateAnnouncements()
        }

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

        if (viewModel.itemCount == 0) {
            val textView = LayoutInflater
                .from(context)
                .inflate(
                    R.layout.inbox_no_announcements_textview,
                    inboxFragmentLayout
                )
        }

        loading = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("scrollY", inboxRecyclerView.computeVerticalScrollOffset())

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()

        viewModel.appRepository.unreadAnnouncementsPushListener = null
    }
}