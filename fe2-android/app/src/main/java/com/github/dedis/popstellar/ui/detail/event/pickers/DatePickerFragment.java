package com.github.dedis.popstellar.ui.detail.event.pickers;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.appcompat.app.AppCompatDialogFragment;

import com.github.dedis.popstellar.R;

import java.util.Calendar;

/**
 * This fragment shows a dialog to choose a date. It takes as argument (set with setArguments) a
 * request key that will be used to give the response.
 *
 * <p>More info : https://developer.android.com/guide/fragments/communicate
 *
 * <p>Help found here (outdated) :
 * https://brandonlehr.com/android/learn-to-code/2018/08/19/callling-android-datepicker-fragment-from-a-fragment-and-getting-the-date
 */
public final class DatePickerFragment extends AppCompatDialogFragment
    implements DatePickerDialog.OnDateSetListener {

  public static final String TAG = DatePickerFragment.class.getSimpleName();
  public static final String REQUEST_KEY = "REQUEST_KEY";

  private final Calendar calendar = Calendar.getInstance();
  private String request;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Query the request key
    request = requireArguments().getString(REQUEST_KEY);

    // Set the current date as the default date
    final Calendar currentCalendar = Calendar.getInstance();
    int year = currentCalendar.get(Calendar.YEAR);
    int month = currentCalendar.get(Calendar.MONTH);
    int day = currentCalendar.get(Calendar.DAY_OF_MONTH);

    // Return a new instance of DatePickerDialog
    return new DatePickerDialog(requireActivity(), DatePickerFragment.this, year, month, day);
  }

  // called when a date has been selected
  public void onDateSet(DatePicker view, int year, int month, int day) {
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.MONTH, month);
    calendar.set(Calendar.DAY_OF_MONTH, day);

    calendar.set(Calendar.HOUR, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    // send date back to the target fragment
    Bundle bundle = new Bundle();
    bundle.putSerializable(request, calendar);
    getParentFragmentManager().setFragmentResult(getString(R.string.picker_selection), bundle);
  }
}
