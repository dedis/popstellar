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
import com.github.dedis.popstellar.ui.detail.event.pickers.PickerConstant;
import com.github.dedis.popstellar.ui.detail.event.pickers.TimePickerFragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Multiples Event Creation Fragment have in common that they implement start/end date and start/end
 * time.
 *
 * <p>This class handles these fields.
 */
public abstract class AbstractEventCreationFragment extends Fragment {
  private static final int ONE_HOUR = 1000 * 60 * 60;

  private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
  private final DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.FRENCH);

  private final Calendar threshold = Calendar.getInstance();
  private final Calendar completeStartTime = Calendar.getInstance();
  private final Calendar completeEndTime = Calendar.getInstance();

  protected long creationTimeInSeconds;
  protected long startTimeInSeconds;
  protected long endTimeInSeconds;

  @Nullable private Calendar startDate;
  @Nullable private Calendar endDate;
  @Nullable private Calendar startTime;
  @Nullable private Calendar endTime;

  private EditText startDateEditText;
  private EditText endDateEditText;
  private EditText startTimeEditText;
  private EditText endTimeEditText;

  // This is to avoid clearing the fields on each click
  private boolean toBeCleared = true;

  public void setDateAndTimeView(View view) {
    long currentMillis = System.currentTimeMillis();
    long suggestedEndMillis = currentMillis + ONE_HOUR; // Adding one hour
    startDate = new GregorianCalendar();
    startTime = new GregorianCalendar();
    Date dCurrent =
        new java.util.Date(currentMillis); // Get's the date based on the unix time stamp
    startDate.setTime(dCurrent);
    startTime.setTime(dCurrent);
    Date dSuggestedEnd = new Date(suggestedEndMillis);
    endDate = new GregorianCalendar();
    endTime = new GregorianCalendar();
    endDate.setTime(dSuggestedEnd);
    endTime.setTime(dSuggestedEnd);

    startDateEditText = view.findViewById(R.id.start_date_edit_text);
    startDateEditText.setInputType(InputType.TYPE_NULL);
    startDateEditText.setText(dateFormat.format(dCurrent));

    endDateEditText = view.findViewById(R.id.end_date_edit_text);
    endDateEditText.setInputType(InputType.TYPE_NULL);
    endDateEditText.setText(dateFormat.format(dSuggestedEnd));

    startTimeEditText = view.findViewById(R.id.start_time_edit_text);
    startTimeEditText.setInputType(InputType.TYPE_NULL);
    startTimeEditText.setText(timeFormat.format(dCurrent));

    endTimeEditText = view.findViewById(R.id.end_time_edit_text);
    endTimeEditText.setInputType(InputType.TYPE_NULL);
    endTimeEditText.setText(timeFormat.format(dSuggestedEnd));

    // Offset the threshold a little to accept current value
    threshold.add(Calendar.MINUTE, -1);

    startDateEditText.setOnClickListener(
        v -> {
          clearDates();
          openPickerDialog(new DatePickerFragment(), DatePickerFragment.TAG, this::onStartDate);
        });

    endDateEditText.setOnClickListener(
        v -> {
          clearDates();
          openPickerDialog(new DatePickerFragment(), DatePickerFragment.TAG, this::onEndDate);
        });

    startTimeEditText.setOnClickListener(
        v -> {
          clearDates();
          openPickerDialog(new TimePickerFragment(), TimePickerFragment.TAG, this::onStartTime);
        });

    endTimeEditText.setOnClickListener(
        v -> {
          clearDates();
          openPickerDialog(new TimePickerFragment(), TimePickerFragment.TAG, this::onEndTime);
        });
  }

  /**
   * When the user want to change the suggested time, it would be cumbersome without clearing the
   * suggested fields. So on the first click on one of those fields we clear them all, but not on
   * subsequent ones
   */
  private void clearDates() {
    // Should only be cleared on the first click on those fields, to erase suggested time
    if (toBeCleared) {
      startDateEditText.getText().clear();
      startTimeEditText.getText().clear();
      endDateEditText.getText().clear();
      endTimeEditText.getText().clear();

      startDate = null;
      startTime = null;
      endDate = null;
      endTime = null;
      toBeCleared = false;
    }
  }

  private void openPickerDialog(
      AppCompatDialogFragment fragment, String fragmentTag, FragmentResultListener listener) {
    // Create Listener
    getParentFragmentManager()
        .setFragmentResultListener(PickerConstant.REQUEST_KEY, getViewLifecycleOwner(), listener);
    // show the picker
    fragment.show(getParentFragmentManager(), fragmentTag);
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

    if (compareWithNowByDay(newDate) < 0) {
      showToast(R.string.past_date_not_allowed);
      return;
    }

    if (endDate != null && newDate.compareTo(endDate) > 0) {
      showToast(R.string.start_date_after_end_date_not_allowed);
      return;
    }

    startDate = newDate;
    startDateEditText.setText(dateFormat.format(startDate.getTime()));

    if (endDate != null && newDate.compareTo(endDate) == 0) {
      endTime = null;
      endTimeEditText.setText("");
    }

    if (compareWithNowByDay(newDate) == 0) {
      computeTimesInSeconds();
    }
  }

  private void onEndDate(String requestKey, Bundle bundle) {
    Calendar newDate = getSelection(bundle);

    endDateEditText.setText("");
    endDate = null;

    if (compareWithNowByDay(newDate) < 0) {
      showToast(R.string.past_date_not_allowed);
      return;
    }

    if ((startDate != null) && (newDate.compareTo(startDate) < 0)) {
      showToast(R.string.end_date_after_start_date_not_allowed);
      return;
    }

    endDate = newDate;
    endDateEditText.setText(dateFormat.format(newDate.getTime()));

    if ((startDate != null) && (startDate.compareTo(newDate) == 0)) {
      endTime = null;
      endTimeEditText.setText("");
    }
  }

  private void onStartTime(String requestKey, Bundle bundle) {
    startTime = getSelection(bundle);
    startTimeEditText.setText(timeFormat.format(startTime.getTime()));

    if (startDate != null
        && endDate != null
        && startDate.compareTo(endDate) == 0
        && endTime != null
        && startTime.compareTo(endTime) > 0) {
      showToast(R.string.start_time_after_end_time_not_allowed);
      startTime = null;
      startTimeEditText.setText("");
    } else if (startDate != null && compareWithNowByDay(startDate) == 0) {
      computeTimesInSeconds();
    }
  }

  private void onEndTime(String requestKey, Bundle bundle) {
    endTime = getSelection(bundle);
    endTimeEditText.setText(timeFormat.format(endTime.getTime()));

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

  private Calendar getSelection(Bundle bundle) {
    Calendar value = (Calendar) bundle.getSerializable(PickerConstant.RESPONSE_KEY);
    if (value == null) throw new IllegalStateException("Bundle does not contain selection");
    return value;
  }

  private int compareWithNowByDay(Calendar date) {
    Calendar dayThreshold =
        new Calendar.Builder()
            .setDate(
                threshold.get(Calendar.YEAR),
                threshold.get(Calendar.MONTH),
                threshold.get(Calendar.DAY_OF_MONTH))
            .build();

    return date.compareTo(dayThreshold);
  }

  private void showToast(@StringRes int text) {
    Toast.makeText(getActivity(), getString(text), Toast.LENGTH_LONG).show();
  }

  /**
   * Compute the creationTimeInSeconds, completeStartTime, completeEndTime. And check if the
   * start/end are still valid. (end can be null in the case of RollCall)
   *
   * @return true if the date/times are all valid
   */
  public boolean computeTimesInSeconds() {
    if (startDate == null || startTime == null) {
      return false;
    }
    completeStartTime.set(
        startDate.get(Calendar.YEAR),
        startDate.get(Calendar.MONTH),
        startDate.get(Calendar.DAY_OF_MONTH),
        startTime.get(Calendar.HOUR_OF_DAY),
        startTime.get(Calendar.MINUTE),
        0);

    Instant creation = Instant.now();
    Instant start = completeStartTime.toInstant();
    if (start.isBefore(creation)) {
      // If the start is more than 5 minutes in the past, invalidate the time
      if (start.plus(5, ChronoUnit.MINUTES).isBefore(creation)) {
        showToast(R.string.past_date_not_allowed);
        startTime = null;
        startTimeEditText.setText("");
        return false;
      } else {
        // Else (if start is only a little in the past), set the start to creation
        start = creation;
        startTimeEditText.setText(timeFormat.format(Date.from(start).getTime()));
      }
    }
    creationTimeInSeconds = creation.getEpochSecond();
    startTimeInSeconds = start.getEpochSecond();
    if (endDate == null ^ endTime == null) {
      return false;
    }
    if (endDate != null) {
      completeEndTime.set(
          endDate.get(Calendar.YEAR),
          endDate.get(Calendar.MONTH),
          endDate.get(Calendar.DAY_OF_MONTH),
          endTime.get(Calendar.HOUR_OF_DAY),
          endTime.get(Calendar.MINUTE),
          0);
      Instant end = completeEndTime.toInstant();
      if (end.isBefore(start)) {
        if (end.truncatedTo(ChronoUnit.MINUTES).equals(start.truncatedTo(ChronoUnit.MINUTES))) {
          // If the endTime was set on the same minute as the start, use same time for start and end
          end = start;
          endTimeEditText.setText(timeFormat.format(Date.from(end).getTime()));
        } else {
          showToast(R.string.past_date_not_allowed);
          endTime = null;
          endTimeEditText.setText("");
          return false;
        }
      }
      endTimeInSeconds = end.getEpochSecond();
      return true;
    }
    endTimeInSeconds = 0;
    return true;
  }
}
