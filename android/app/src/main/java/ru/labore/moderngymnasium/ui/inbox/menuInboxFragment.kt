package ru.labore.moderngymnasium.ui.inbox

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.menu_inbox_fragment.*
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.base.ScopedFragment

class menuInboxFragment : ScopedFragment(), DIAware {
    override val di: DI by lazy { (context as DIAware).di }

    private val viewModelFactory: MenuInboxViewModelFactory by instance()
    private val repository: AppRepository by instance()

    private lateinit var viewModel: MenuInboxViewModel

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

        mainTextView.text = "Loading..."

        bindUI()
    }

    private fun bindUI() = launch {
        val announcements = viewModel.announcements.await()
        var text = ""

        announcements.forEach {
            text = text.plus(it.toString())
        }

        mainTextView.text = if (text.isEmpty()) "No results found" else text
    }
}