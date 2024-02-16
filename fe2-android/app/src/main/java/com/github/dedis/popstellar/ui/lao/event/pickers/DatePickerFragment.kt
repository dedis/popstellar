package com.github.dedis.popstellar.ui.lao.event.pickers

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

/**
 * This fragment shows a dialog to choose a date. It takes as argument (set with setArguments) a
 * request key that will be used to give the response.
 *
 * More info : https://developer.android.com/guide/fragments/communicate
 *
 * Help found here (outdated) :
 * https://brandonlehr.com/android/learn-to-code/2018/08/19/callling-android-datepicker-fragment-from-a-fragment-and-getting-the-date
 */
@AndroidEntryPoint
class DatePickerFragment : AppCompatDialogFragment(), OnDateSetListener {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    // Set the current date as the default date
    val current = Calendar.getInstance()
    val year = current[Calendar.YEAR]
    val month = current[Calendar.MONTH]
    val day = current[Calendar.DAY_OF_MONTH]

    // Return a new instance of DatePickerDialog
    return DatePickerDialog(requireActivity(), this@DatePickerFragment, year, month, day)
  }

  // called when a date has been selected
  override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
    val calendar = Calendar.Builder().setDate(year, month, day).build()

    // send date back to the target fragment
    val bundle = Bundle()
    bundle.putSerializable(PickerConstant.RESPONSE_KEY, calendar)

    parentFragmentManager.setFragmentResult(PickerConstant.REQUEST_KEY, bundle)
  }

  companion object {
    val TAG: String = DatePickerFragment::class.java.simpleName

    fun newInstance(): DatePickerFragment {
      return DatePickerFragment()
    }
  }
}
