package ru.labore.moderngymnasium.ui.create

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.menu_create_fragment.*
import kotlinx.coroutines.launch
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.base.ScopedFragment

class MenuCreateFragment : ScopedFragment(), DIAware {
    override val di by lazy { (context as DIAware).di }

    private val viewModelFactory:
            MenuCreateViewModelFactory by instance()

    private lateinit var viewModel: MenuCreateViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.menu_create_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders
            .of(this, viewModelFactory)
            .get(MenuCreateViewModel::class.java)

        createAnnouncementButton.setOnClickListener {createAnnouncement()}
    }

    private fun createAnnouncement() {
        val text = createAnnouncementEditText
            .text
            .toString()
            .trim()

        if (text.isEmpty()) {
            Toast.makeText(
                this.requireContext(),
                getString(R.string.enter_announcement_text),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            launch {
                viewModel.createAnnouncement(text)

                Toast.makeText(
                    this@MenuCreateFragment.requireContext(),
                    "Got it! Check your server response.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}