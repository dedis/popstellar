package com.github.dedis.popstellar.ui.detail.event.pickers;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Calendar;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * This fragment shows a dialog to choose a time. It takes as argument (set with setArguments) a *
 * request key that will be used to give the response.
 *
 * <p>More info : https://developer.android.com/guide/fragments/communicate
 */
@AndroidEntryPoint
public final class TimePickerFragment extends AppCompatDialogFragment
    implements TimePickerDialog.OnTimeSetListener {

  public static final String TAG = TimePickerFragment.class.getSimpleName();

  public static TimePickerFragment newInstance() {
    return new TimePickerFragment();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Calendar current = Calendar.getInstance();
    int hourOfDay = current.get(Calendar.HOUR_OF_DAY);
    int minute = current.get(Calendar.MINUTE);

    return new TimePickerDialog(getActivity(), this, hourOfDay, minute, true);
  }

  /**
   * Called when the user is done setting a new time and the dialog has closed.
   *
   * @param view the view associated with this listener
   * @param hourOfDay the hour that was set
   * @param minute the minute that was set
   */
  @Override
  public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
    Calendar calendar = new Calendar.Builder().setTimeOfDay(hourOfDay, minute, 0).build();

    Bundle bundle = new Bundle();
    bundle.putSerializable(PickerConstant.RESPONSE_KEY, calendar);
    getParentFragmentManager().setFragmentResult(PickerConstant.REQUEST_KEY, bundle);
  }
}
