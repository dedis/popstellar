package com.github.dedis.popstellar.ui.lao.event

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.ui.lao.event.pickers.DatePickerFragment
import com.github.dedis.popstellar.ui.lao.event.pickers.PickerConstant
import com.github.dedis.popstellar.ui.lao.event.pickers.TimePickerFragment
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Arrays
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Multiples Event Creation Fragment have in common that they implement start/end date and start/end
 * time.
 *
 * This class handles these fields.
 */
abstract class AbstractEventCreationFragment : Fragment() {
  private val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
  private val timeFormat: DateFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)

  private val threshold = Calendar.getInstance()
  private val completeStartTime = threshold
  private val completeEndTime = threshold

  @JvmField protected var creationTimeInSeconds: Long = 0
  @JvmField protected var startTimeInSeconds: Long = 0
  @JvmField protected var endTimeInSeconds: Long = 0

  private var startDate: Calendar? = null
  private var endDate: Calendar? = null

  private var startTime: Calendar? = null
  private var endTime: Calendar? = null

  private var startDateEditText: EditText? = null
  private var endDateEditText: EditText? = null

  private var startTimeEditText: EditText? = null
  private var endTimeEditText: EditText? = null

  @JvmField protected var confirmButton: Button? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    if (confirmButton != null && !confirmButton!!.hasOnClickListeners()) {
      confirmButton!!.setOnClickListener { createEvent() }
    }
  }

  protected abstract fun createEvent()

  fun setDateAndTimeView(view: View) {
    val currentMillis = System.currentTimeMillis()
    val suggestedEndMillis = currentMillis + ONE_HOUR // Adding one hour

    startDate = GregorianCalendar()
    startTime = GregorianCalendar()

    val dCurrent = Date(currentMillis) // Gets the date based on the unix time stamp
    (startDate as GregorianCalendar).time = dCurrent
    (startTime as GregorianCalendar).time = dCurrent

    endDate = GregorianCalendar()
    endTime = GregorianCalendar()

    val dSuggestedEnd = Date(suggestedEndMillis)
    (endDate as GregorianCalendar).time = dSuggestedEnd
    (endTime as GregorianCalendar).time = dSuggestedEnd

    startDateEditText = view.findViewById(R.id.start_date_edit_text)
    startDateEditText?.inputType = InputType.TYPE_NULL
    startDateEditText?.setText(dateFormat.format(dCurrent))

    endDateEditText = view.findViewById(R.id.end_date_edit_text)
    endDateEditText?.inputType = InputType.TYPE_NULL
    endDateEditText?.setText(dateFormat.format(dSuggestedEnd))

    startTimeEditText = view.findViewById(R.id.start_time_edit_text)
    startTimeEditText?.inputType = InputType.TYPE_NULL
    startTimeEditText?.setText(timeFormat.format(dCurrent))

    endTimeEditText = view.findViewById(R.id.end_time_edit_text)
    endTimeEditText?.inputType = InputType.TYPE_NULL
    endTimeEditText?.setText(timeFormat.format(dSuggestedEnd))

    // Offset the threshold a little to accept current value
    threshold.add(Calendar.MINUTE, -1)
    startDateEditText?.setOnClickListener {
      // When the user click on start date we clear start and end dates
      startDateEditText?.text?.clear()
      startDate = null

      endDateEditText?.text?.clear()
      endDate = null

      openPickerDialog(DatePickerFragment.newInstance(), DatePickerFragment.TAG) {
        request: String,
        bundle: Bundle ->
        onStartDate(request, bundle)
      }
    }

    endDateEditText?.setOnClickListener {
      // When the user click on end date we only clear end dates
      endDateEditText?.text?.clear()
      endDate = null

      openPickerDialog(DatePickerFragment.newInstance(), DatePickerFragment.TAG) {
        requestKey: String,
        bundle: Bundle ->
        onEndDate(requestKey, bundle)
      }
    }

    startTimeEditText?.setOnClickListener {
      // When the user clicks on start time we clear both start time and end time
      startTimeEditText?.text?.clear()
      startTime = null

      endTimeEditText?.text?.clear()
      endTime = null

      openPickerDialog(TimePickerFragment.newInstance(), TimePickerFragment.TAG) {
        requestKey: String,
        bundle: Bundle ->
        onStartTime(requestKey, bundle)
      }
    }

    endTimeEditText?.setOnClickListener {
      // When the user clicks on end time we only clear end time
      endTimeEditText?.text?.clear()
      endTime = null

      openPickerDialog(TimePickerFragment(), TimePickerFragment.TAG) {
        requestKey: String,
        bundle: Bundle ->
        onEndTime(requestKey, bundle)
      }
    }
  }

  private fun openPickerDialog(
    fragment: AppCompatDialogFragment,
    fragmentTag: String,
    listener: FragmentResultListener
  ) {
    // Create Listener
    parentFragmentManager.setFragmentResultListener(
      PickerConstant.REQUEST_KEY,
      viewLifecycleOwner,
      listener
    )
    // show the picker
    fragment.show(parentFragmentManager, fragmentTag)
  }

  fun addStartDateAndTimeListener(listener: TextWatcher?) {
    startTimeEditText?.addTextChangedListener(listener)
    startDateEditText?.addTextChangedListener(listener)
  }

  fun addEndDateAndTimeListener(listener: TextWatcher?) {
    endTimeEditText?.addTextChangedListener(listener)
    endDateEditText?.addTextChangedListener(listener)
  }

  fun getStartDate(): String {
    return startDateEditText?.text.toString().trim { it <= ' ' }
  }

  fun getStartTime(): String {
    return startTimeEditText?.text.toString().trim { it <= ' ' }
  }

  fun getEndDate(): String {
    return endDateEditText?.text.toString().trim { it <= ' ' }
  }

  fun getEndTime(): String {
    return endTimeEditText?.text.toString().trim { it <= ' ' }
  }

  private fun onStartDate(request: String, bundle: Bundle) {
    val newDate = getSelection(bundle)
    startDateEditText?.setText("")
    startDate = null

    if (compareWithNowByDay(newDate) < 0) {
      showToast(R.string.past_date_not_allowed)
      return
    }

    if (endDate != null && newDate > endDate!!) {
      showToast(R.string.start_date_after_end_date_not_allowed)
      return
    }

    startDate = newDate
    startDateEditText?.setText(dateFormat.format(startDate!!.time))
    if (newDate == endDate) {
      endTime = null
      endTimeEditText?.setText("")
    }

    if (compareWithNowByDay(newDate) == 0) {
      computeTimesInSeconds()
    }
  }

  private fun onEndDate(requestKey: String, bundle: Bundle) {
    val newDate = getSelection(bundle)
    endDateEditText?.setText("")
    endDate = null

    if (compareWithNowByDay(newDate) < 0) {
      showToast(R.string.past_date_not_allowed)
      return
    }

    if (startDate != null && newDate < startDate!!) {
      showToast(R.string.end_date_after_start_date_not_allowed)
      return
    }

    endDate = newDate
    endDateEditText?.setText(dateFormat.format(newDate.time))

    if (startDate == newDate) {
      endTime = null
      endTimeEditText?.setText("")
    }
  }

  private fun onStartTime(requestKey: String, bundle: Bundle) {
    startTime = getSelection(bundle)
    startTimeEditText?.setText(timeFormat.format(startTime!!.time))

    if (
      startDate != null &&
        endDate != null &&
        startDate == endDate &&
        endTime != null &&
        startTime!! > endTime!!
    ) {
      showToast(R.string.start_time_after_end_time_not_allowed)
      startTime = null
      startTimeEditText?.setText("")
    } else if (startDate != null && compareWithNowByDay(startDate!!) == 0) {
      computeTimesInSeconds()
    }
  }

  private fun onEndTime(requestKey: String, bundle: Bundle) {
    endTime = getSelection(bundle)
    endTimeEditText?.setText(timeFormat.format(endTime!!.time))

    if (
      startDate != null &&
        endDate != null &&
        startDate == endDate &&
        startTime != null &&
        startTime!! > endTime!!
    ) {
      showToast(R.string.end_time_before_start_time_not_allowed)
      endTime = null
      endTimeEditText?.setText("")
    }
  }

  private fun getSelection(bundle: Bundle): Calendar {
    return bundle.getSerializable(PickerConstant.RESPONSE_KEY) as Calendar?
      ?: throw IllegalStateException("Bundle does not contain selection")
  }

  private fun compareWithNowByDay(date: Calendar): Int {
    val dayThreshold =
      Calendar.Builder()
        .setDate(
          threshold[Calendar.YEAR],
          threshold[Calendar.MONTH],
          threshold[Calendar.DAY_OF_MONTH]
        )
        .build()

    return date.compareTo(dayThreshold)
  }

  private fun showToast(@StringRes text: Int) {
    Toast.makeText(activity, getString(text), Toast.LENGTH_LONG).show()
  }

  /**
   * Compute the creationTimeInSeconds, completeStartTime, completeEndTime. And check if the
   * start/end are still valid. (end can be null in the case of RollCall)
   *
   * @return true if the date/times are all valid
   */
  fun computeTimesInSeconds(): Boolean {
    if (startDate == null || startTime == null) {
      return false
    }

    completeStartTime[
      startDate!![Calendar.YEAR],
      startDate!![Calendar.MONTH],
      startDate!![Calendar.DAY_OF_MONTH],
      startTime!![Calendar.HOUR_OF_DAY],
      startTime!![Calendar.MINUTE]] = 0
    val creation = Instant.now()
    var start = completeStartTime.toInstant()
    if (start.isBefore(creation)) {
      // If the start is more than 5 minutes in the past, invalidate the time
      if (start.plus(5, ChronoUnit.MINUTES).isBefore(creation)) {
        showToast(R.string.past_date_not_allowed)
        startTime = null
        startTimeEditText?.setText("")
        return false
      } else {
        // Else (if start is only a little in the past), set the start to creation
        start = creation
        startTimeEditText?.setText(timeFormat.format(Date.from(start).time))
      }
    }

    creationTimeInSeconds = creation.epochSecond
    startTimeInSeconds = start.epochSecond

    if ((endDate == null) xor (endTime == null)) {
      return false
    }

    if (endDate != null) {
      completeEndTime[
        endDate!![Calendar.YEAR],
        endDate!![Calendar.MONTH],
        endDate!![Calendar.DAY_OF_MONTH],
        endTime!![Calendar.HOUR_OF_DAY],
        endTime!![Calendar.MINUTE]] = 0
      var end = completeEndTime.toInstant()
      if (end.isBefore(start)) {
        if (end.truncatedTo(ChronoUnit.MINUTES) == start.truncatedTo(ChronoUnit.MINUTES)) {
          // If the endTime was set on the same minute as the start, use same time for start and end
          end = start
          endTimeEditText?.setText(timeFormat.format(Date.from(end).time))
        } else {
          showToast(R.string.past_date_not_allowed)
          endTime = null
          endTimeEditText?.setText("")
          return false
        }
      }

      endTimeInSeconds = end.epochSecond
      return true
    }

    endTimeInSeconds = 0
    return true
  }

  /**
   * Function which enables the confirm button based on the required fields to be filled.
   *
   * @param requiredTexts variable length parameters representing the EditText required to confirm
   * @return the TextWatcher object which enables dynamically the confirm button
   */
  protected fun getConfirmTextWatcher(vararg requiredTexts: EditText): TextWatcher {
    return object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // Nothing needed here
      }

      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        // Check that all text fields are not empty
        val areAllFieldsFilled =
          Arrays.stream(requiredTexts).noneMatch { text: EditText ->
            text.text.toString().isEmpty()
          }
        confirmButton?.isEnabled =
          areAllFieldsFilled && getStartDate().isNotEmpty() && getStartTime().isNotEmpty()
      }

      override fun afterTextChanged(s: Editable) {
        // Nothing needed here
      }
    }
  }

  companion object {
    private const val ONE_HOUR = 1000 * 60 * 60
  }
}
