package ru.labore.moderngymnasium.ui.activities

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_announcement_create.*
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import ru.labore.moderngymnasium.R
import ru.labore.moderngymnasium.data.network.ClientConnectionException
import ru.labore.moderngymnasium.data.network.ClientErrorException
import ru.labore.moderngymnasium.data.repository.AppRepository
import ru.labore.moderngymnasium.ui.base.ScopedActivity
import ru.labore.moderngymnasium.ui.views.LabelledCheckbox
import ru.labore.moderngymnasium.ui.views.ParentCheckbox
import ru.labore.moderngymnasium.utils.hideKeyboard
import java.net.ConnectException
import java.util.*
import kotlin.math.hypot


class AnnouncementCreateActivity : ScopedActivity(), DIAware {
    override val di: DI by lazy { (applicationContext as DIAware).di }
    private val checkedRoles: HashMap<Int, MutableList<Int>> = hashMapOf()
    private val repository: AppRepository by instance()
    private var announcementText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        setContentView(R.layout.activity_announcement_create)

        if (savedInstanceState == null) {
            activityCreateLayout.visibility = View.INVISIBLE
            val viewTreeObserver: ViewTreeObserver = activityCreateLayout.viewTreeObserver

            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        circularRevealActivity()

                        activityCreateLayout
                            .viewTreeObserver
                            .removeOnGlobalLayoutListener(this)
                    }
                })
            }
        }

        loadUI()

        activityCreateLayout.setOnClickListener { hideKeyboard() }
        createAnnouncementRoleChoose.setOnClickListener { hideKeyboard() }
        createAnnouncementButton.setOnClickListener { createAnnouncement() }
    }

    private fun circularRevealActivity(directionForwards: Boolean = true) {
        val cx: Int = intent.extras?.getInt("x") ?: 0
        val cy: Int = intent.extras?.getInt("y") ?: 0
        val firstRadius: Float = intent.extras?.getFloat("radius") ?: 0F
        val secondRadius = hypot(
            activityCreateLayout.width.toDouble(),
            activityCreateLayout.height.toDouble()
        ).toFloat()
        val circularReveal: Animator

        if (directionForwards) {
            circularReveal = ViewAnimationUtils.createCircularReveal(
                activityCreateLayout,
                cx,
                cy,
                firstRadius,
                secondRadius
            )

            activityCreateLayout.visibility = View.VISIBLE
        } else {
            circularReveal = ViewAnimationUtils.createCircularReveal(
                activityCreateLayout,
                cx,
                cy,
                secondRadius,
                0F
            )

            circularReveal.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    finish()
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationRepeat(animation: Animator?) {
                }

            })
        }
        circularReveal.duration = 600
        circularReveal.start()
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
                            this@AnnouncementCreateActivity,
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
                            this@AnnouncementCreateActivity,
                            "${
                                when (Locale.getDefault().language) {
                                    "ru" -> role.nameRu
                                    else -> role.name
                                }
                            }, ${classes[firstGrade]!![0].grade}"
                        )

                        classes[firstGrade]!!.forEach { classEntity ->
                            val nestedCheckbox = LabelledCheckbox(
                                this@AnnouncementCreateActivity,
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
                    this@AnnouncementCreateActivity,
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
                                this@AnnouncementCreateActivity,
                                "${classes[it]!![0].grade}${classes[it]!![0].letter}"
                            )
                        } else {
                            childCheckbox = ParentCheckbox(
                                this@AnnouncementCreateActivity,
                                "${classes[it]!![0].grade}"
                            )

                            classes[it]!!.forEach { classEntity ->
                                val nestedCheckbox = LabelledCheckbox(
                                    this@AnnouncementCreateActivity,
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
                val textView = TextView(this@AnnouncementCreateActivity)
                textView.text = getString(R.string.no_rights_to_announce)
                textView.gravity = Gravity.CENTER_HORIZONTAL

                createAnnouncementRoleChoose.addView(textView)
            }
        }
    }

    private fun createAnnouncement() {
        if (checkedRoles.keys.size == 0) {
            Toast.makeText(
                this,
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
                    this,
                    getString(R.string.enter_announcement_text),
                    Toast.LENGTH_SHORT
                ).show()
            } else launch {
                try {
                    repository.createAnnouncement(text, checkedRoles)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@AnnouncementCreateActivity,
                        when (e) {
                            is ConnectException -> getString(R.string.server_unavailable)
                            is ClientConnectionException -> getString(R.string.no_internet)
                            is ClientErrorException -> {
                                if (e.errorCode == AppRepository.HTTP_RESPONSE_CODE_UNAUTHORIZED) {
                                    repository.user = null
                                    startActivity(
                                        Intent(
                                            this@AnnouncementCreateActivity,
                                            LoginActivity::class.java
                                        )
                                    )
                                    finish()
                                }

                                getString(R.string.invalid_credentials)
                            }
                            else -> "An unknown error occurred."
                        },
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        circularRevealActivity(false)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}