package ru.labore.moderngymnasium.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.menu_inbox_fragment.*
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.ui.adapters.MainRecyclerViewAdapter
import ru.labore.moderngymnasium.ui.base.ScopedFragment

class MenuInboxFragment : ScopedFragment(), DIAware {
    override val di: DI by lazy { (context as DIAware).di }

    private val viewModelFactory: MenuInboxViewModelFactory by instance()
    private val viewManager: LinearLayoutManager by lazy {
        LinearLayoutManager(requireActivity())
    }

    private lateinit var viewModel: MenuInboxViewModel
    private lateinit var viewAdapter: MainRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.menu_inbox_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders
            .of(this, viewModelFactory)
            .get(MenuInboxViewModel::class.java)

        bindUI()
    }

    private fun bindUI() = launch {
        val announcements = viewModel.announcements.await()

        Toast.makeText(requireActivity(), announcements.amount.toString(), Toast.LENGTH_SHORT).show()

        inboxProgressBar.visibility = View.GONE
        inboxProgressBarCaption.visibility = View.GONE

        viewAdapter = MainRecyclerViewAdapter(resources, announcements.data)

        inboxRecyclerView.apply {
            setHasFixedSize(true)

            layoutManager = viewManager
            adapter = viewAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollVertically(1)) {
                        Toast.makeText(requireActivity(), "Reached!", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}