package ru.labore.moderngymnasium.ui.create

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_create.*
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.network.ClientConnectionException
import ru.labore.moderngymnasium.data.network.ClientErrorException
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.activities.LoginActivity
import ru.labore.moderngymnasium.ui.activities.MainActivity
import ru.labore.moderngymnasium.ui.base.ScopedFragment
import ru.labore.moderngymnasium.ui.views.LabelledCheckbox
import ru.labore.moderngymnasium.ui.views.ParentCheckbox
import ru.labore.moderngymnasium.utils.hideKeyboard
import java.net.ConnectException
import java.util.*
import kotlin.math.hypot


class CreateFragment : ScopedFragment(), DIAware {
    override val di: DI by lazy { (requireContext() as DIAware).di }
    private val checkedRoles: HashMap<Int, MutableList<Int>> = hashMapOf()
    private val repository: AppRepository by instance()
    private var announcementText: String? = null
    private var UILoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        createFragmentLayout.setOnClickListener { hideKeyboard() }
        createAnnouncementRoleChoose.setOnClickListener { hideKeyboard() }
        createAnnouncementSubmitButton.setOnClickListener { createAnnouncement() }
        createAnnouncementBackButton.setOnClickListener {
            val location = IntArray(2)
            val activity = requireActivity() as MainActivity
            val radius = hypot(
                activity.rootMainLayout.width.toDouble(),
                activity.rootMainLayout.height.toDouble()
            ).toFloat()

            it.getLocationInWindow(location)

            val circularReveal = ViewAnimationUtils.createCircularReveal(
                createFragmentLayout,
                location[0] + it.width / 2,
                location[1] - it.height / 2,
                radius,
                0F
            )

            circularReveal.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    activity.createFragment.visibility = View.GONE
                    circularReveal.removeAllListeners()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    circularReveal.removeAllListeners()
                }

                override fun onAnimationRepeat(animation: Animator?) {
                }

            })

            circularReveal.duration = 600
            circularReveal.start()
        }
    }

    fun reveal (x: Int, y: Int, radius: Float) {
        val activity = requireActivity() as MainActivity
        val secondRadius = hypot(
            activity.rootMainLayout.width.toDouble(),
            activity.rootMainLayout.height.toDouble()
        ).toFloat()

        val circularReveal: Animator = ViewAnimationUtils.createCircularReveal(
            createFragmentLayout,
            x,
            y,
            radius,
            secondRadius
        )

        activity.createFragment.visibility = View.VISIBLE

        circularReveal.duration = 600
        circularReveal.start()

        if (!UILoaded) {
            loadUI()
            UILoaded = true
        }
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
        val roles = repository.getUserRoles().filterNotNull()
        val classes = repository.getUserClasses()
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
                                when (Locale.getDefault().language) {
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
                                when (Locale.getDefault().language) {
                                    "ru" -> role.nameRu
                                    else -> role.name
                                }
                            }, ${classes[firstGrade]!![0].grade}"
                        )

                        classes[firstGrade]!!.forEach { classEntity ->
                            val nestedCheckbox = LabelledCheckbox(
                                activity,
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
                    activity,
                    when (Locale.getDefault().language) {
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
                                activity,
                                "${classes[it]!![0].grade}${classes[it]!![0].letter}"
                            )
                        } else {
                            childCheckbox = ParentCheckbox(
                                activity,
                                "${classes[it]!![0].grade}"
                            )

                            classes[it]!!.forEach { classEntity ->
                                val nestedCheckbox = LabelledCheckbox(
                                    activity,
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
                val textView = TextView(activity)
                textView.text = getString(R.string.no_rights_to_announce)
                textView.gravity = Gravity.CENTER_HORIZONTAL

                createAnnouncementRoleChoose.addView(textView)
            }
        }
    }

    private fun createAnnouncement() {
        val activity = requireActivity()

        if (checkedRoles.keys.size == 0) {
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
            } else launch {
                try {
                    createAnnouncementSubmitButton.visibility = View.GONE
                    createAnnouncementSubmitProgress.visibility = View.VISIBLE

                    repository.createAnnouncement(text, checkedRoles)

                    createAnnouncementEditText.setText("")
                } catch (e: Exception) {
                    Toast.makeText(
                        activity,
                        when (e) {
                            is ConnectException -> getString(R.string.server_unavailable)
                            is ClientConnectionException -> getString(R.string.no_internet)
                            is ClientErrorException -> {
                                if (e.errorCode == AppRepository.HTTP_RESPONSE_CODE_UNAUTHORIZED) {
                                    repository.user = null
                                    startActivity(
                                        Intent(
                                            activity,
                                            LoginActivity::class.java
                                        )
                                    )
                                    activity.finish()
                                }

                                getString(R.string.invalid_credentials)
                            }
                            else -> "An unknown error occurred."
                        },
                        Toast.LENGTH_LONG
                    ).show()
                }

                createAnnouncementSubmitButton.visibility = View.VISIBLE
                createAnnouncementSubmitProgress.visibility = View.GONE
            }
        }
    }
}