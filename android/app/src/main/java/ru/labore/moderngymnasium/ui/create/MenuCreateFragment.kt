package ru.labore.moderngymnasium.ui.create

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.menu_create_fragment.*
import kotlinx.coroutines.launch
import org.kodein.di.DIAware
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.network.ClientConnectionException
import ru.labore.moderngymnasium.data.network.ClientErrorException
import ru.labore.moderngymnasium.ui.base.ScopedFragment
import ru.labore.moderngymnasium.ui.views.LabelledCheckbox
import ru.labore.moderngymnasium.ui.views.ParentCheckbox
import ru.labore.moderngymnasium.utils.hideKeyboard
import java.net.ConnectException
import java.util.*

class MenuCreateFragment : ScopedFragment(), DIAware {
    override val di by lazy { (context as DIAware).di }

    private val viewModel: MenuCreateViewModel by viewModels()
    private val checkedRoles: HashMap<Int, MutableList<Int>> = hashMapOf()

    private var announcementText: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.menu_create_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        loadUI()

        fragmentCreateLayout.setOnClickListener { hideKeyboard() }
        createAnnouncementRoleChoose.setOnClickListener { hideKeyboard() }
        createAnnouncementButton.setOnClickListener { createAnnouncement() }
    }

    private fun childCheckedChangeHandler(isChecked: Boolean, roleId: Int, classId: Int) {
        if (checkedRoles.containsKey(roleId)) {
            if (isChecked) {
                if (
                    checkedRoles[roleId]
                        ?.contains(classId) == false
                ) {
                    checkedRoles[roleId]?.add(classId)
                }
            } else {
                checkedRoles[roleId]?.remove(classId)
                if (checkedRoles[roleId]?.isEmpty() == true) {
                    checkedRoles.remove(roleId)
                }
            }
        } else {
            checkedRoles[roleId] = mutableListOf()

            if (isChecked) {
                checkedRoles[roleId]!!.add(classId)
            } else {
                checkedRoles[roleId]!!.remove(classId)
            }
        }
    }

    private fun loadUI() = launch {
        val roles = viewModel.roles.await()
        val classes = viewModel.classes.await()
        val activity = requireActivity()

        if (announcementText != null) {
            createAnnouncementEditText.setText(announcementText)
        }

        roles.forEach { role ->
            val checkboxLayout: View

            if (classes.size == 1) {
                val firstGrade = classes.keys.elementAt(0)

                if (
                    classes[firstGrade]?.size != null &&
                    classes[firstGrade]?.size!! > 0
                ) {
                    if (classes[firstGrade]?.size == 1) {
                        checkboxLayout = LabelledCheckbox(
                            activity,
                            "${
                                when(Locale.getDefault().language) {
                                    "ru" -> role.nameRu
                                    else -> role.name
                                }
                            }, ${classes[firstGrade]!![0].grade}${classes[firstGrade]!![0].letter}"
                        )

                        checkboxLayout.checkedChangeHandler = { checked ->
                            childCheckedChangeHandler(
                                checked,
                                role.id,
                                classes[firstGrade]!![0].id
                            )
                        }

                        createAnnouncementRoleChoose.addView(checkboxLayout)
                    } else {
                        checkboxLayout = ParentCheckbox(
                            activity,
                            "${
                                when(Locale.getDefault().language) {
                                    "ru" -> role.nameRu
                                    else -> role.name
                                }
                            }, ${classes[firstGrade]!![0].grade}"
                        )

                        classes[firstGrade]!!.forEach { classEntity ->
                            val nestedCheckbox = LabelledCheckbox(
                                requireActivity(),
                                classEntity.letter
                            )

                            nestedCheckbox.checkedChangeHandler = { checked ->
                                childCheckedChangeHandler(checked, role.id, classEntity.id)
                            }

                            checkboxLayout.addView(nestedCheckbox)
                        }
                    }
                }
            } else if (classes.size > 1) {
                checkboxLayout = ParentCheckbox(
                    requireActivity(),
                    when(Locale.getDefault().language) {
                        "ru" -> role.nameRu
                        else -> role.name
                    }
                )

                classes.keys.forEach {
                    if (
                        classes[it]?.size != null &&
                        classes[it]!!.size > 0
                    ) {
                        val childCheckbox: View

                        if (classes[it]!!.size == 1) {
                            childCheckbox = LabelledCheckbox(
                                requireActivity(),
                                "${classes[it]!![0].grade}${classes[it]!![0].letter}"
                            )
                        } else {
                            childCheckbox = ParentCheckbox(
                                requireActivity(),
                                "${classes[it]!![0].grade}"
                            )

                            classes[it]!!.forEach { classEntity ->
                                val nestedCheckbox = LabelledCheckbox(
                                    requireActivity(),
                                    classEntity.letter
                                )

                                nestedCheckbox.checkedChangeHandler = { checked ->
                                    childCheckedChangeHandler(checked, role.id, classEntity.id)
                                }

                                childCheckbox.addView(nestedCheckbox)
                            }
                        }

                        checkboxLayout.addView(childCheckbox)
                        createAnnouncementRoleChoose.addView(checkboxLayout)
                    }
                }
            } else {
                val textView = TextView(requireActivity())
                textView.text = getString(R.string.no_rights_to_announce)
                textView.gravity = Gravity.CENTER_HORIZONTAL

                createAnnouncementRoleChoose.addView(textView)
            }
        }
    }

    private fun createAnnouncement() {
        println(checkedRoles.toString())
        if (checkedRoles.keys.size == 0) {
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
                    try {
                        viewModel.createAnnouncement(text, checkedRoles)

                        Toast.makeText(
                            this@MenuCreateFragment.requireContext(),
                            "Got it! Check your server response.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch(e: Exception) {
                        println(e.message)

                        val toastString: String = when (e) {
                            is ConnectException -> getString(R.string.server_unavailable)
                            is ClientConnectionException -> getString(R.string.no_internet)
                            is ClientErrorException -> getString(R.string.invalid_credentials)
                            else -> "Look into the console."
                        }

                        Toast.makeText(
                            requireActivity(),
                            toastString,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}