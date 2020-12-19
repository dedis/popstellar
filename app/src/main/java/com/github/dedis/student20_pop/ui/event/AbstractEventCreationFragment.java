package com.github.dedis.student20_pop.ui.event;

import android.app.Activity;
import android.content.Intent;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.ui.DatePickerFragment;
import com.github.dedis.student20_pop.ui.TimePickerFragment;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Multiples Event Creation Fragment have in common
 * that they implement start/end date and start/end time.
 * <p>
 * This class handles these fields.
 */
abstract class AbstractEventCreationFragment extends Fragment {
    public static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.FRENCH);
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
    public static final int START_DATE_REQUEST_CODE = 11; //Used to identify the request
    public static final int END_DATE_REQUEST_CODE = 12;
    public static final int START_TIME_REQUEST_CODE = 13;
    public static final int END_TIME_REQUEST_CODE = 14;
    public static Date startDate;
    public static Date endDate;
    public static Date startTime;
    public static Date endTime;
    public static Date today;
    private EditText startDateEditText;
    private EditText endDateEditText;
    private EditText startTimeEditText;
    private EditText endTimeEditText;
    private String selection;

    public void setDateAndTimeView(View view, Fragment fragment, FragmentManager fragmentManager) {
        startDateEditText = view.findViewById(R.id.start_date_editText);
        startDateEditText.setInputType(InputType.TYPE_NULL);

        endDateEditText = view.findViewById(R.id.end_date_editText);
        endDateEditText.setInputType(InputType.TYPE_NULL);

        startTimeEditText = view.findViewById(R.id.start_time_editText);
        startTimeEditText.setInputType(InputType.TYPE_NULL);

        endTimeEditText = view.findViewById(R.id.end_time_editText);
        endTimeEditText.setInputType(InputType.TYPE_NULL);

        startDateEditText.setOnClickListener(v -> {
            // create the datePickerFragment
            AppCompatDialogFragment datePickerFragment = new DatePickerFragment();
            // set the targetFragment to receive the results, specifying the request code
            datePickerFragment.setTargetFragment(fragment, START_DATE_REQUEST_CODE);
            // show the datePicker
            datePickerFragment.show(fragmentManager, DatePickerFragment.TAG);
        });

        endDateEditText.setOnClickListener(v -> {
            AppCompatDialogFragment datePickerFragment = new DatePickerFragment();
            datePickerFragment.setTargetFragment(fragment, END_DATE_REQUEST_CODE);
            datePickerFragment.show(fragmentManager, DatePickerFragment.TAG);
        });

        startTimeEditText.setOnClickListener(v -> {
            AppCompatDialogFragment timePickerFragment = new TimePickerFragment();
            timePickerFragment.setTargetFragment(fragment, START_TIME_REQUEST_CODE);
            timePickerFragment.show(fragmentManager, TimePickerFragment.TAG);
        });

        endTimeEditText.setOnClickListener(v -> {
            AppCompatDialogFragment timePickerFragment = new TimePickerFragment();
            timePickerFragment.setTargetFragment(fragment, END_TIME_REQUEST_CODE);
            timePickerFragment.show(fragmentManager, TimePickerFragment.TAG);
        });
    }

    public void addDateAndTimeListener(TextWatcher listener) {
        startTimeEditText.addTextChangedListener(listener);
        startDateEditText.addTextChangedListener(listener);
    }

    public String getStartDate() {
        return startDateEditText.getText().toString().trim();
    }

    public String getStartTime() {
        return startTimeEditText.getText().toString().trim();
    }

    public void checkDates(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            selection = data.getStringExtra(getString(R.string.picker_selection));
            try {
                switch (requestCode) {
                    case START_DATE_REQUEST_CODE:
                        startDate = DATE_FORMAT.parse(selection);
                        /*
                          In Java, two dates can be compared using the compareTo() method of Comparable interface.
                          This method returns '0' if both the dates are equal,
                          it returns a value "greater than 0" if date1 is after date2 and
                          it returns a value "less than 0" if date1 is before date2.
                         */
                        if (startDate.compareTo(today) < 0) {
                            Toast.makeText(getActivity(),
                                    getString(R.string.past_date_not_allowed),
                                    Toast.LENGTH_LONG).show();
                            startDateEditText.setText("");
                            startDate = null;
                        } else {
                            if ((endDate != null) && (startDate.compareTo(endDate) > 0)) {
                                Toast.makeText(getActivity(),
                                        getString(R.string.start_date_after_end_date_not_allowed),
                                        Toast.LENGTH_LONG).show();
                                startDateEditText.setText("");
                                startDate = null;
                            } else {
                                startDateEditText.setText(DATE_FORMAT.format(startDate));
                                startDate = DATE_FORMAT.parse(selection);
                                if ((endDate != null) && (startDate.compareTo(endDate) == 0)) {
                                    endTime = null;
                                    endTimeEditText.setText("");
                                }
                            }
                        }
                        break;

                    case END_DATE_REQUEST_CODE:
                        endDate = DATE_FORMAT.parse(selection);
                        if (endDate.compareTo(today) < 0) {
                            Toast.makeText(getActivity(),
                                    getString(R.string.past_date_not_allowed),
                                    Toast.LENGTH_LONG).show();
                            endDateEditText.setText("");
                            endDate = null;
                        } else {
                            if ((startDate != null) && (endDate.compareTo(startDate) < 0)) {
                                Toast.makeText(getActivity(),
                                        getString(R.string.end_date_after_start_date_not_allowed),
                                        Toast.LENGTH_SHORT).show();
                                endDateEditText.setText("");
                                endDate = null;
                            } else {
                                if ((startDate != null) && (startDate.compareTo(endDate) == 0)) {
                                    endTime = null;
                                    endTimeEditText.setText("");
                                }
                                endDateEditText.setText(DATE_FORMAT.format(endDate));
                                endDate = DATE_FORMAT.parse(selection);
                            }
                        }
                        break;

                    case START_TIME_REQUEST_CODE:
                        if (startDate == null || endDate == null) {
                            startTimeEditText.setText(selection);
                        } else {
                            startTime = TIME_FORMAT.parse(selection);
                            if ((startDate.compareTo(endDate) == 0) &&
                                    (endTime != null) &&
                                    (startTime.compareTo(endTime) > 0)) {
                                Toast.makeText(getActivity(),
                                        getString(R.string.start_time_after_end_time_not_allowed),
                                        Toast.LENGTH_SHORT).show();
                                startTime = null;
                                startTimeEditText.setText("");
                            } else {
                                startTimeEditText.setText(selection);
                            }
                        }

                        break;

                    case END_TIME_REQUEST_CODE:
                        if ((startDate == null) || (endDate == null)) {
                            endTimeEditText.setText(selection);
                        } else {
                            endTime = TIME_FORMAT.parse(selection);
                            if ((startDate.compareTo(endDate) == 0) &&
                                    (startTime != null) &&
                                    (endTime.compareTo(startTime) < 0)) {
                                Toast.makeText(getActivity(),
                                        getString(R.string.end_time_before_start_time_not_allowed),
                                        Toast.LENGTH_SHORT).show();
                                endTime = null;
                                endTimeEditText.setText("");
                            } else {
                                endTimeEditText.setText(selection);
                            }
                        }
                        break;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

    }
}
