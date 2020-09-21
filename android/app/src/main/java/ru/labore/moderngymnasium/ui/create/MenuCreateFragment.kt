package ru.labore.moderngymnasium.ui.create

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.menu_create_fragment.*
import kotlinx.coroutines.launch
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.ui.base.ScopedFragment
import java.util.*

class MenuCreateFragment : ScopedFragment(), DIAware {
    override val di by lazy { (context as DIAware).di }

    private val viewModelFactory:
            MenuCreateViewModelFactory by instance()

    private var checkedRoles: MutableList<Int>? = null
    private var announcementText: String? = null
    private lateinit var viewModel: MenuCreateViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.menu_create_fragment, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkedRoles = savedInstanceState
            ?.getIntArray("checked_roles")
            ?.toList() as MutableList<Int>?

        announcementText = savedInstanceState
            ?.getString("announcement_text")

        viewModel = ViewModelProviders
            .of(this, viewModelFactory)
            .get(MenuCreateViewModel::class.java)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putIntArray("checked_roles", checkedRoles?.toIntArray())
        outState.putString("announcement_text", createAnnouncementEditText.text.toString())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        loadUI()

        createAnnouncementButton.setOnClickListener {createAnnouncement()}
    }

    private fun loadUI() = launch {
        val roles = viewModel.roles.await()
        val activity = requireActivity()

        if (announcementText != null) {
            createAnnouncementEditText.setText(announcementText)
        }

        if (checkedRoles == null) {
            checkedRoles = MutableList(roles.size) {
                roles[it].id
            }
        }

        for (it in roles) {
            val checkboxLayout = LinearLayout(activity)
            val checkboxCaption = TextView(activity)
            val checkbox = CheckBox(activity)

            checkboxLayout.orientation = LinearLayout.HORIZONTAL
            checkboxLayout.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            checkbox.isChecked = checkedRoles!!.indexOf(it.id) != -1
            checkbox.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0F
            )
            checkboxCaption.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1F
            )

            checkboxCaption.text = when(Locale.getDefault().language) {
                "ru" -> it.nameRu
                else -> it.name
            }

            checkbox.setOnClickListener {view ->
                if ((view as CheckBox).isChecked) {
                    checkedRoles!!.add(it.id)
                } else {
                    checkedRoles!!.remove(it.id)
                }
            }

            checkboxLayout.addView(checkboxCaption)
            checkboxLayout.addView(checkbox)

            createAnnouncementRoleChoose.addView(checkboxLayout)
        }
    }

    private fun createAnnouncement() {
        if (checkedRoles == null) {
            loadUI()
        } else {
            if (checkedRoles!!.isEmpty()) {
                Toast.makeText(
                    requireActivity(),
                    getString(R.string.choose_recipient_role),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val text = createAnnouncementEditText
                    .text
                    .toString()
                    .trim()

                if (text.isEmpty()) {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.enter_announcement_text),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    launch {
                        viewModel.createAnnouncement(text, checkedRoles!!.toTypedArray())

                        Toast.makeText(
                            this@MenuCreateFragment.requireContext(),
                            "Got it! Check your server response.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}