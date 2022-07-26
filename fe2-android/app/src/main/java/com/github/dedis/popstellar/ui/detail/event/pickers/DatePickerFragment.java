package com.github.dedis.popstellar.ui.detail.event.pickers;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

import androidx.appcompat.app.AppCompatDialogFragment;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * This fragment shows a dialog to choose a date. It takes as argument (set with setArguments) a
 * request key that will be used to give the response.
 *
 * <p>More info : https://developer.android.com/guide/fragments/communicate
 *
 * <p>Help found here (outdated) :
 * https://brandonlehr.com/android/learn-to-code/2018/08/19/callling-android-datepicker-fragment-from-a-fragment-and-getting-the-date
 */
@AndroidEntryPoint
public final class DatePickerFragment extends AppCompatDialogFragment
    implements DatePickerDialog.OnDateSetListener {

  public static final String TAG = DatePickerFragment.class.getSimpleName();

  public static DatePickerFragment newInstance() {
    return new DatePickerFragment();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Set the current date as the default date
    final Calendar current = Calendar.getInstance();
    int year = current.get(Calendar.YEAR);
    int month = current.get(Calendar.MONTH);
    int day = current.get(Calendar.DAY_OF_MONTH);

    // Return a new instance of DatePickerDialog
    return new DatePickerDialog(requireActivity(), DatePickerFragment.this, year, month, day);
  }

  // called when a date has been selected
  public void onDateSet(DatePicker view, int year, int month, int day) {
    Calendar calendar = new Calendar.Builder().setDate(year, month, day).build();

    // send date back to the target fragment
    Bundle bundle = new Bundle();
    bundle.putSerializable(PickerConstant.RESPONSE_KEY, calendar);
    getParentFragmentManager().setFragmentResult(PickerConstant.REQUEST_KEY, bundle);
  }
}
