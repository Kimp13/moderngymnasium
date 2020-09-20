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
import androidx.core.widget.addTextChangedListener
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

    private lateinit var viewModel: MenuCreateViewModel
    private lateinit var checkedRoles: MutableList<Int>

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

        loadRoles()

        createAnnouncementButton.setOnClickListener {createAnnouncement()}
    }

    private fun loadRoles() = launch {
        val roles = viewModel.roles.await()
        val activity = requireActivity()
        checkedRoles = MutableList(roles.size) {
            roles[it].id
        }

        val roleCheckboxes = Array(roles.size) { it ->
            val checkboxLayout = LinearLayout(activity)
            val checkboxCaption = TextView(activity)
            val checkbox = CheckBox(activity)

            checkboxLayout.orientation = LinearLayout.HORIZONTAL
            checkboxLayout.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            checkbox.isChecked = true
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
                "ru" -> roles[it].nameRu
                else -> roles[it].name
            }

            checkbox.setOnClickListener {view ->
                if ((view as CheckBox).isChecked) {
                    checkedRoles.add(roles[it].id)
                } else {
                    checkedRoles.remove(roles[it].id)
                }
            }

            checkboxLayout.addView(checkboxCaption)
            checkboxLayout.addView(checkbox)

            createAnnouncementRoleChoose.addView(checkboxLayout)

            checkbox
        }
    }

    private fun createAnnouncement() {
        if (checkedRoles.isEmpty()) {
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
                    viewModel.createAnnouncement(text, checkedRoles.toTypedArray())

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