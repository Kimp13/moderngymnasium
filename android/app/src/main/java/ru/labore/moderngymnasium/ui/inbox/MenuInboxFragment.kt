package ru.labore.moderngymnasium.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.menu_inbox_fragment.*
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.entities.AnnouncementEntity
import ru.labore.moderngymnasium.ui.adapters.MainRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.ScopedFragment
import kotlin.properties.Delegates

class MenuInboxFragment : ScopedFragment(), DIAware {
    override val di: DI by lazy { (context as DIAware).di }

    private val viewModel: MenuInboxViewModel by viewModels()
    private val viewManager: LinearLayoutManager by lazy {
        LinearLayoutManager(requireActivity())
    }

    private var loading = true
    private var overallCount by Delegates.notNull<Int>()
    private var currentCount by Delegates.notNull<Int>()
    private lateinit var viewAdapter: MainRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.menu_inbox_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        bindUI()

        viewModel.appRepository.inboxAnnouncement.observe(viewLifecycleOwner) {
            viewAdapter.prependAnnouncement(it)
        }
    }

    private fun addNewAnnouncements() = launch {
        if (!loading) {
            loading = true
            println("Setting to visible.")
            inboxProgressBar.visibility = View.VISIBLE

            val newAnnouncements = viewModel.getAnnouncements(currentCount)

            currentCount += newAnnouncements.currentCount

            viewAdapter.pushAnnouncements(newAnnouncements.data)

            loading = false
            println("Setting to gone.")
            inboxProgressBar.visibility = View.GONE
        }
    }

    private fun refreshUI() = launch {
        val announcements = viewModel
            .appRepository
            .getAnnouncements(0, 25, true)

        currentCount = announcements.currentCount
        overallCount = announcements.overallCount

        viewAdapter.refreshAnnouncements(announcements.data)

        inboxRefreshLayout.isRefreshing = false
    }

    private fun bindUI() = launch {
        val announcements = viewModel.announcements.await()
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

        viewAdapter = MainRecyclerViewAdapter(
            resources,
            announcements.data.toList() as MutableList<AnnouncementEntity>
        )

        inboxRefreshLayout.setOnRefreshListener {
            refreshUI()
        }

        inboxRecyclerView.apply {
            setHasFixedSize(true)

            layoutManager = viewManager
            adapter = viewAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (
                        !recyclerView.canScrollVertically(1) &&
                        currentCount < overallCount
                    ) {
                        addNewAnnouncements()
                    }
                }
            })
        }
    }
}