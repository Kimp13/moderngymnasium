package ru.labore.eventeger.ui.fragments.create

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.fragment_create.*
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import ru.labore.eventeger.R
import ru.labore.eventeger.ui.base.BaseFragment
import ru.labore.eventeger.ui.fragments.DatePickerFragment
import ru.labore.eventeger.ui.fragments.TimePickerFragment
import ru.labore.eventeger.ui.views.LabelledCheckbox
import ru.labore.eventeger.ui.views.ParentCheckbox
import ru.labore.eventeger.utils.hideKeyboard
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.arrayListOf
import kotlin.collections.forEach
import kotlin.collections.hashSetOf
import kotlin.collections.set

class CreateFragment : BaseFragment() {
    companion object Derived {
        private val timeFilter = IntentFilter()

        init {
            timeFilter.addAction(Intent.ACTION_TIME_TICK)
            timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
            timeFilter.addAction(Intent.ACTION_TIME_CHANGED)
        }
    }

    override val viewModel: CreateViewModel by viewModels()
    private val timeChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            writeStartHeader()
            writeEndHeader()
        }
    }
    private val startDatePicker: DatePickerFragment
    private val startTimePicker: TimePickerFragment
    private val endDatePicker: DatePickerFragment
    private val endTimePicker: TimePickerFragment
    private var startDateTime: ZonedDateTime? = null
        set(value) {
            if (value == null) {
                field = null
            } else if (
                endDateTime != null && (
                        value.isAfter(endDateTime) ||
                                value.isEqual(endDateTime)
                        )
            ) {
                field = endDateTime!!.minusMinutes(1)
            } else {
                field = value.withSecond(0).withNano(0)

                startDatePicker.updateDate(
                    field!!.year,
                    field!!.monthValue,
                    field!!.dayOfMonth
                )
                startTimePicker.updateTime(
                    field!!.hour,
                    field!!.minute
                )

                endDatePicker.minDate = if (
                    field!!.hour == 23 &&
                    field!!.minute == 59
                )
                    field!!.plusMinutes(1).toEpochSecond() * 1000
                else
                    field!!.toEpochSecond() * 1000
            }

            writeStartHeader()
        }
    private var endDateTime: ZonedDateTime? = null
        set(value) {
            if (value == null) {
                field = null
            } else if (
                startDateTime != null && (
                        value.isBefore(startDateTime) ||
                                value.isEqual(startDateTime)
                        )
            ) {
                field = startDateTime!!.plusMinutes(1)
            } else {
                field = value.withSecond(0).withNano(0)

                endDatePicker.updateDate(
                    field!!.year,
                    field!!.monthValue,
                    field!!.dayOfMonth
                )
                endTimePicker.updateTime(
                    field!!.hour,
                    field!!.minute
                )

                startDatePicker.maxDate = if (
                    field!!.hour == 0 &&
                    field!!.minute == 0
                )
                    field!!.minusMinutes(1).toEpochSecond() * 1000
                else
                    field!!.toEpochSecond() * 1000
            }

            writeEndHeader()
        }
    private var isEvent: Boolean = false

    init {
        startDatePicker = DatePickerFragment { year, month, day ->
            startDateTime = (startDateTime ?: viewModel.appRepository.zonedNow())
                .withYear(year)
                .withMonth(month + 1)
                .withDayOfMonth(day)
        }

        startTimePicker = TimePickerFragment { hour, minute ->
            startDateTime = (startDateTime ?: viewModel.appRepository.zonedNow())
                .withHour(hour)
                .withMinute(minute)
        }

        endDatePicker = DatePickerFragment { year, month, day ->
            endDateTime = (endDateTime ?: viewModel.appRepository.zonedNow())
                .withYear(year)
                .withMonth(month + 1)
                .withDayOfMonth(day)
        }

        endTimePicker = TimePickerFragment { hour, minute ->
            endDateTime = (endDateTime ?: viewModel.appRepository.zonedNow())
                .withHour(hour)
                .withMinute(minute)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createEvent.switchListener = {
            isEvent = it
        }

        val minDate = ZonedDateTime.of(
            2021,
            1,
            1,
            0,
            0,
            0,
            0,
            ZoneId.systemDefault()
        ).toEpochSecond() * 1000

        startDatePicker.minDate = minDate
        endDatePicker.minDate = minDate

        val progressBar = ProgressBar(activity)
        progressBar.isIndeterminate = true

        launch {
            loadUI()

            createRecipientsLoading?.visibility = View.GONE
            createRecipientsProgressBar?.visibility = View.GONE
        }

        createFragmentScrollView?.setOnClickListener { hideKeyboard() }
        createFragmentParametersLayout?.setOnClickListener { hideKeyboard() }
        createFragmentParametersLayout?.children?.forEach {
            it.setOnClickListener { hideKeyboard() }
        }

        createStartDate?.setOnClickListener {
            startDatePicker.show(parentFragmentManager, "startDatePicker")
        }

        createStartTime?.setOnClickListener {
            startTimePicker.show(parentFragmentManager, "startTimePicker")
        }

        createEndDate?.setOnClickListener {
            endDatePicker.show(parentFragmentManager, "endDatePicker")
        }

        createEndTime?.setOnClickListener {
            endTimePicker.show(parentFragmentManager, "endTimePicker")
        }

        writeStartHeader()
        writeEndHeader()
    }

    override fun onResume() {
        super.onResume()

        createFragmentSubmitButton.setOnClickListener {
            println("I'm clicked")
            it.visibility = View.GONE
            createFragmentProgressBar.visibility = View.VISIBLE

            createAnnouncement {
                it.visibility = View.VISIBLE
                createFragmentProgressBar.visibility = View.GONE
            }
        }

        activity?.registerReceiver(timeChangedReceiver, timeFilter)
    }

    override fun onPause() {
        super.onPause()

        activity?.unregisterReceiver(timeChangedReceiver)
    }

    private fun createAnnouncement(
        afterAll: () -> Unit
    ) {
        if (viewModel.checkedRoles.keys.size == 0) {
            Toast.makeText(
                activity,
                getString(R.string.choose_recipients),
                Toast.LENGTH_SHORT
            ).show()

            afterAll()
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

                afterAll()
            } else {
                val act = activity

                if (act != null)
                    launch {
                        if (isEvent)
                            viewModel.createAnnouncement(
                                act,
                                text,
                                startDateTime,
                                endDateTime
                            )
                        else
                            viewModel.createAnnouncement(
                                act,
                                text
                            )

                        createAnnouncementEditText?.setText("")

                        afterAll()
                    }
            }
        }
    }

    private fun childCheckedChangeHandler(isChecked: Boolean, roleId: Int, classId: Int) {
        if (!viewModel.checkedRoles.containsKey(roleId))
            viewModel.checkedRoles[roleId] = hashSetOf()

        if (isChecked)
            viewModel.checkedRoles[roleId]!!.add(classId)
        else
            viewModel.checkedRoles[roleId]!!.remove(classId)
    }

    private suspend fun loadUI() {
        val act = activity

        if (act != null) {
            val roles = viewModel.getRoles(act)
            val classes = viewModel.getClasses(act)

            viewModel.appRepository.announceMap.entries.forEach { roleMap ->
                val role = roles[roleMap.key]

                if (role != null && roleMap.value.size > 0) {
                    val checkboxLayout: View

                    if (roleMap.value.size == 1) {
                        val onlyClass = classes[roleMap.value[0]]!!

                        checkboxLayout = LabelledCheckbox(
                            act,
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
                            act,
                            "Роль: ${role.name}"
                        )

                        gradedClasses.entries.forEach {
                            val childCheckbox: View

                            if (it.value.size == 1) {
                                val onlyClass = classes[it.value[0]]!!

                                childCheckbox = LabelledCheckbox(
                                    act,
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
                                    act,
                                    "${it.key}-я параллель"
                                )

                                it.value.forEach { classId ->
                                    val leafCheckbox = LabelledCheckbox(
                                        act,
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

    private fun writeHeader(
        header: TextView?,
        nullId: Int,
        futureFmtId: Int,
        pastFmtId: Int,
        date: ZonedDateTime?
    ) {
        if (header != null) {
            header.text =
                if (date == null) {
                    resources.getString(
                        nullId
                    )
                } else {
                    val now = viewModel.appRepository.zonedNow()

                    resources.getString(
                        if (date.isBefore(now) || date.isEqual(now)) pastFmtId else futureFmtId,
                        if (date.dayOfMonth < 10) "0${date.dayOfMonth}" else date.dayOfMonth,
                        if (date.monthValue < 10) "0${date.monthValue}" else date.monthValue,
                        date.year,
                        if (date.hour < 10) "0${date.hour}" else date.hour,
                        if (date.minute < 10) "0${date.minute}" else date.minute
                    )
                }
        }
    }

    private fun writeStartHeader() {
        writeHeader(
            createStartHeader,
            R.string.starts_now,
            R.string.starts_at_fmt_verbose,
            R.string.started_at_fmt_verbose,
            startDateTime
        )
    }

    private fun writeEndHeader() {
        writeHeader(
            createEndHeader,
            R.string.never_ends,
            R.string.ends_at_fmt_verbose,
            R.string.ended_at_fmt_verbose,
            endDateTime
        )
    }
}
