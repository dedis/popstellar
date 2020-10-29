package com.github.dedis.student20_pop.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.appcompat.app.AppCompatDialogFragment;

import com.github.dedis.student20_pop.R;

import java.util.Calendar;

import static com.github.dedis.student20_pop.ui.event.MeetingEventFragment.DATE_FORMAT;


/**
 * Help found here :
 * https://brandonlehr.com/android/learn-to-code/2018/08/19/callling-android-datepicker-fragment-from-a-fragment-and-getting-the-date
 */
public class DatePickerFragment extends AppCompatDialogFragment implements DatePickerDialog.OnDateSetListener {
    public static final String TAG = DatePickerFragment.class.getSimpleName();
    private final Calendar calendar = Calendar.getInstance();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Set the current date as the default date
        final Calendar currentCalendar = Calendar.getInstance();
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);
        int day = currentCalendar.get(Calendar.DAY_OF_MONTH);

        // Return a new instance of DatePickerDialog
        return new DatePickerDialog(getActivity(), DatePickerFragment.this, year, month, day);
    }

    // called when a date has been selected
    public void onDateSet(DatePicker view, int year, int month, int day) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        String selectedDate = DATE_FORMAT.format(calendar.getTime());

        // send date back to the target fragment
        getTargetFragment().onActivityResult(
                getTargetRequestCode(),
                Activity.RESULT_OK,
                new Intent().putExtra(getString(R.string.picker_selection), selectedDate)
        );
    }
}