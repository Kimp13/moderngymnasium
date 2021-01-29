package ru.labore.moderngymnasium.ui.fragments.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.fragment_create.*
import kotlinx.coroutines.launch
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.db.entities.ClassEntity
import ru.labore.moderngymnasium.data.db.entities.RoleEntity
import ru.labore.moderngymnasium.ui.base.ListElementFragment
import ru.labore.moderngymnasium.ui.views.LabelledCheckbox
import ru.labore.moderngymnasium.ui.views.ParentCheckbox
import ru.labore.moderngymnasium.utils.hideKeyboard

class CreateFragment(
    controls: ListElementFragment.Companion.ListElementFragmentControls
) : ListElementFragment(controls) {
    override val viewModel: CreateViewModel by viewModels()
    private var uiLoaded = false
    private var lastScroll = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        createFragmentScrollView
            .viewTreeObserver
            .addOnScrollChangedListener {
                val scroll = createFragmentScrollView.scrollY
                val difference = scroll - lastScroll

                if (difference > 0)
                    controls.hideBottomNav()
                else if (difference < 0)
                    controls.showBottomNav()

                lastScroll = scroll
            }

        createFragmentRootLayout.setOnClickListener { hideKeyboard() }
        createAnnouncementRoleChoose.setOnClickListener { hideKeyboard() }
        createAnnouncementSubmitButton.setOnClickListener { createAnnouncement() }
        createAnnouncementBackButton.setOnClickListener {
            controls.finish()
        }

        loadUI()
    }

    private fun createAnnouncement() {
        if (viewModel.checkedRoles.keys.size == 0) {
            Toast.makeText(
                activity,
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
                    activity,
                    getString(R.string.enter_announcement_text),
                    Toast.LENGTH_SHORT
                ).show()
            } else makeRequest({
                createAnnouncementSubmitButton.visibility = View.GONE
                createAnnouncementSubmitProgress.visibility = View.VISIBLE

                viewModel.createAnnouncement(text)

                createAnnouncementEditText.setText("")

                createAnnouncementSubmitButton.visibility = View.VISIBLE
                createAnnouncementSubmitProgress.visibility = View.GONE
            })
        }
    }

    private fun childCheckedChangeHandler(isChecked: Boolean, roleId: Int, classId: Int) {
        if (!viewModel.checkedRoles.containsKey(roleId))
            viewModel.checkedRoles[roleId] = hashSetOf()

        if (isChecked)
            viewModel.checkedRoles[roleId]!!.add(classId)
        else
            viewModel.checkedRoles[roleId]!!.remove(classId)

        println(viewModel.checkedRoles.toString())
    }

    private fun loadUI() = launch {
        var roles = HashMap<Int, RoleEntity>()
        var classes = HashMap<Int, ClassEntity>()

        makeRequest({
            roles = viewModel.getRoles()

            classes = viewModel.getClasses()
        }).join()

        viewModel.appRepository.announceMap.entries.forEach { roleMap ->
            val role = roles[roleMap.key]
            val checkboxLayout: View
            val context = context

            if (role != null && context != null) {
                if (roleMap.value.size == 1) {
                    val onlyClass = classes[roleMap.value[0]]!!

                    checkboxLayout = LabelledCheckbox(
                        context,
                        "${role.name}, ${
                            onlyClass.grade
                        }${
                            onlyClass.letter
                        } класс"
                    )

                    checkboxLayout.outerCheckedChangeHandler = { checked ->
                        childCheckedChangeHandler(
                            checked,
                            role.id,
                            roleMap.value[0]
                        )
                    }
                } else {
                    val gradedClasses = HashMap<Int, ArrayList<Int>>()

                    roleMap.value.contents.forEach { classId ->
                        val classEntity = classes[classId]

                        if (classEntity != null) {
                            if (gradedClasses.containsKey(classEntity.grade)) {
                                gradedClasses[classEntity.grade]!!.add(classId)
                            } else {
                                gradedClasses[classEntity.grade] = arrayListOf(classId)
                            }
                        }
                    }

                    checkboxLayout = ParentCheckbox(
                        context,
                        "Роль: ${role.name}"
                    )

                    gradedClasses.entries.forEach {
                        val childCheckbox: View

                        if (it.value.size == 1) {
                            val onlyClass = classes[it.value[0]]!!

                            childCheckbox = LabelledCheckbox(
                                context,
                                "${
                                    onlyClass.grade
                                }${
                                    onlyClass.letter
                                } класс"
                            )

                            childCheckbox.outerCheckedChangeHandler = { checked ->
                                childCheckedChangeHandler(
                                    checked,
                                    roleMap.key,
                                    onlyClass.id
                                )
                            }
                        } else {
                            childCheckbox = ParentCheckbox(
                                context,
                                "${it.key}-я параллель"
                            )

                            it.value.forEach { classId ->
                                val leafCheckbox = LabelledCheckbox(
                                    context,
                                    "${classes[classId]!!.letter} класс"
                                )

                                leafCheckbox.outerCheckedChangeHandler = { checked ->
                                    childCheckedChangeHandler(
                                        checked,
                                        roleMap.key,
                                        classId
                                    )
                                }

                                childCheckbox.checkboxLayout.addView(leafCheckbox)
                            }
                        }

                        checkboxLayout.checkboxLayout.addView(childCheckbox)
                    }
                }

                createAnnouncementRoleChoose.addView(checkboxLayout)
            }
        }

    }
}