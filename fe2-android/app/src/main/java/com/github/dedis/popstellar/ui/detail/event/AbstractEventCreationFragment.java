package com.github.dedis.popstellar.ui.detail.event;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.event.pickers.DatePickerFragment;
import com.github.dedis.popstellar.ui.detail.event.pickers.TimePickerFragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Locale;

/**
 * Multiples Event Creation Fragment have in common that they implement start/end date and start/end
 * time.
 *
 * <p>This class handles these fields.
 */
public abstract class AbstractEventCreationFragment extends Fragment {

  public static final String START_DATE_REQUEST_KEY = "START_DATE"; // Used to identify the request
  public static final String END_DATE_REQUEST_KEY = "END_DATE";
  public static final String START_TIME_REQUEST_KEY = "START_TIME";
  public static final String END_TIME_REQUEST_KEY = "END_TIME";

  public static final long CREATION_TIME_IN_SECONDS = Instant.now().getEpochSecond();

  public final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
  public final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.FRENCH);

  private final Calendar today = Calendar.getInstance();
  private final Calendar completeStartTime = Calendar.getInstance();
  private final Calendar completeEndTime = Calendar.getInstance();

  public long startTimeInSeconds;
  public long endTimeInSeconds;

  @Nullable private Calendar startDate;
  @Nullable private Calendar endDate;
  @Nullable private Calendar startTime;
  @Nullable private Calendar endTime;

  private EditText startDateEditText;
  private EditText endDateEditText;
  private EditText startTimeEditText;
  private EditText endTimeEditText;

  public void setDateAndTimeView(View view) {
    startDateEditText = view.findViewById(R.id.start_date_edit_text);
    startDateEditText.setInputType(InputType.TYPE_NULL);

    endDateEditText = view.findViewById(R.id.end_date_edit_text);
    endDateEditText.setInputType(InputType.TYPE_NULL);

    startTimeEditText = view.findViewById(R.id.start_time_edit_text);
    startTimeEditText.setInputType(InputType.TYPE_NULL);

    endTimeEditText = view.findViewById(R.id.end_time_edit_text);
    endTimeEditText.setInputType(InputType.TYPE_NULL);

    today.set(Calendar.HOUR, 0);
    today.set(Calendar.SECOND, 0);
    today.set(Calendar.MINUTE, 0);
    today.set(Calendar.MILLISECOND, 0);

    startDateEditText.setOnClickListener(
        v ->
            openPickerDialog(
                new DatePickerFragment(),
                DatePickerFragment.TAG,
                DatePickerFragment.REQUEST_KEY,
                START_DATE_REQUEST_KEY,
                this::onStartDate));

    endDateEditText.setOnClickListener(
        v ->
            openPickerDialog(
                new DatePickerFragment(),
                DatePickerFragment.TAG,
                DatePickerFragment.REQUEST_KEY,
                END_DATE_REQUEST_KEY,
                this::onEndDate));

    startTimeEditText.setOnClickListener(
        v ->
            openPickerDialog(
                new TimePickerFragment(),
                TimePickerFragment.TAG,
                TimePickerFragment.REQUEST_KEY,
                START_TIME_REQUEST_KEY,
                this::onStartTime));

    endTimeEditText.setOnClickListener(
        v ->
            openPickerDialog(
                new TimePickerFragment(),
                TimePickerFragment.TAG,
                TimePickerFragment.REQUEST_KEY,
                END_TIME_REQUEST_KEY,
                this::onEndTime));
  }

  private void openPickerDialog(
      AppCompatDialogFragment fragment,
      String fragmentTag,
      String bundleKey,
      String requestKey,
      FragmentResultListener listener) {
    // Create Listener
    getParentFragmentManager()
        .setFragmentResultListener(requestKey, getViewLifecycleOwner(), listener);
    // create the fragment
    setArg(fragment, bundleKey, requestKey);
    // show the picker
    fragment.show(getParentFragmentManager(), fragmentTag);
  }

  private void setArg(Fragment fragment, String key, String value) {
    Bundle bundle = new Bundle();
    bundle.putString(key, value);
    fragment.setArguments(bundle);
  }

  private Calendar getSelection(Bundle bundle) {
    Calendar value = (Calendar) bundle.getSerializable(getString(R.string.picker_selection));
    if (value == null) throw new IllegalStateException("Bundle does not contain selection");

    return value;
  }

  public void addStartDateAndTimeListener(TextWatcher listener) {
    startTimeEditText.addTextChangedListener(listener);
    startDateEditText.addTextChangedListener(listener);
  }

  public void addEndDateAndTimeListener(TextWatcher listener) {
    endTimeEditText.addTextChangedListener(listener);
    endDateEditText.addTextChangedListener(listener);
  }

  public String getStartDate() {
    return startDateEditText.getText().toString().trim();
  }

  public String getStartTime() {
    return startTimeEditText.getText().toString().trim();
  }

  public String getEndDate() {
    return endDateEditText.getText().toString().trim();
  }

  public String getEndTime() {
    return endTimeEditText.getText().toString().trim();
  }

  private void onStartDate(String request, Bundle bundle) {
    Calendar newDate = getSelection(bundle);

    startDateEditText.setText("");
    startDate = null;

    if (newDate.compareTo(today) < 0) {
      showToast(R.string.past_date_not_allowed);
      return;
    }

    if (endDate != null && newDate.compareTo(endDate) > 0) {
      showToast(R.string.start_date_after_end_date_not_allowed);
      return;
    }

    startDate = newDate;
    startDateEditText.setText(DATE_FORMAT.format(startDate.getTime()));

    if (endDate != null && newDate.compareTo(endDate) == 0) {
      endTime = null;
      endTimeEditText.setText("");
    }
  }

  private void onEndDate(String requestKey, Bundle bundle) {
    Calendar newDate = getSelection(bundle);

    endDateEditText.setText("");
    endDate = null;

    if (newDate.compareTo(today) < 0) {
      showToast(R.string.past_date_not_allowed);
      return;
    }

    if ((startDate != null) && (newDate.compareTo(startDate) < 0)) {
      showToast(R.string.end_date_after_start_date_not_allowed);
      return;
    }

    if ((startDate != null) && (startDate.compareTo(newDate) == 0)) return;

    endDate = newDate;
    endDateEditText.setText(DATE_FORMAT.format(newDate.getTime()));
  }

  private void onStartTime(String requestKey, Bundle bundle) {
    startTime = getSelection(bundle);
    startTimeEditText.setText(TIME_FORMAT.format(startTime.getTime()));

    if (startDate != null
        && endDate != null
        && startDate.compareTo(endDate) == 0
        && endTime != null
        && startTime.compareTo(endTime) > 0) {
      showToast(R.string.start_time_after_end_time_not_allowed);
      startTime = null;
      startTimeEditText.setText("");
    }
  }

  private void onEndTime(String requestKey, Bundle bundle) {
    endTime = getSelection(bundle);
    endTimeEditText.setText(TIME_FORMAT.format(endTime.getTime()));

    if (startDate != null
        && endDate != null
        && startDate.compareTo(endDate) == 0
        && startTime != null
        && startTime.compareTo(endTime) > 0) {
      showToast(R.string.end_time_before_start_time_not_allowed);
      endTime = null;
      endTimeEditText.setText("");
    }
  }

  private void showToast(@StringRes int text) {
    Toast.makeText(getActivity(), getString(text), Toast.LENGTH_LONG).show();
  }

  public void computeTimesInSeconds() {
    if (startDate == null) {
      startDate = Calendar.getInstance();
    }
    if (startTime == null) {
      startTime = Calendar.getInstance();
    }
    completeStartTime.set(
        startDate.get(Calendar.YEAR),
        startDate.get(Calendar.MONTH),
        startDate.get(Calendar.DAY_OF_MONTH),
        startTime.get(Calendar.HOUR_OF_DAY),
        startTime.get(Calendar.MINUTE));
    Instant start = Instant.ofEpochMilli(completeStartTime.getTimeInMillis());
    startTimeInSeconds = start.getEpochSecond();
    if (endDate != null) {
      if (endTime == null) {
        completeEndTime.set(
            endDate.get(Calendar.YEAR),
            endDate.get(Calendar.MONTH),
            endDate.get(Calendar.DAY_OF_MONTH));
      } else {
        completeEndTime.set(
            endDate.get(Calendar.YEAR),
            endDate.get(Calendar.MONTH),
            endDate.get(Calendar.DAY_OF_MONTH),
            endTime.get(Calendar.HOUR_OF_DAY),
            endTime.get(Calendar.MINUTE));
      }
      Instant end = Instant.ofEpochMilli(completeEndTime.getTimeInMillis());
      endTimeInSeconds = end.getEpochSecond();
    }
  }
}
