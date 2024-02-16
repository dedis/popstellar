package com.github.dedis.popstellar.ui.lao.event.pickers

import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.os.Bundle
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

/**
 * This fragment shows a dialog to choose a time. It takes as argument (set with setArguments) a *
 * request key that will be used to give the response.
 *
 * More info : https://developer.android.com/guide/fragments/communicate
 */
@AndroidEntryPoint
class TimePickerFragment : AppCompatDialogFragment(), OnTimeSetListener {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val current = Calendar.getInstance()
    val hourOfDay = current[Calendar.HOUR_OF_DAY]
    val minute = current[Calendar.MINUTE]

    return TimePickerDialog(activity, this, hourOfDay, minute, true)
  }

  /**
   * Called when the user is done setting a new time and the dialog has closed.
   *
   * @param view the view associated with this listener
   * @param hourOfDay the hour that was set
   * @param minute the minute that was set
   */
  override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
    val calendar = Calendar.Builder().setTimeOfDay(hourOfDay, minute, 0).build()
    val bundle = Bundle()
    bundle.putSerializable(PickerConstant.RESPONSE_KEY, calendar)

    parentFragmentManager.setFragmentResult(PickerConstant.REQUEST_KEY, bundle)
  }

  companion object {
    val TAG: String = TimePickerFragment::class.java.simpleName

    fun newInstance(): TimePickerFragment {
      return TimePickerFragment()
    }
  }
}
