package com.github.dedis.popstellar.ui.detail.event.pickers;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatDialogFragment;

import com.github.dedis.popstellar.R;

import java.util.Calendar;

public final class TimePickerFragment extends AppCompatDialogFragment
    implements TimePickerDialog.OnTimeSetListener {

  public static final String TAG = TimePickerFragment.class.getSimpleName();
  public static final String REQUEST_KEY = "REQUEST_KEY";

  private final Calendar calendar = Calendar.getInstance();
  private String request;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // Query the request key
    request = requireArguments().getString(REQUEST_KEY);

    int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);

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
    calendar.setTimeInMillis(0L);
    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
    calendar.set(Calendar.MINUTE, minute);

    Bundle bundle = new Bundle();
    bundle.putSerializable(request, calendar);
    getParentFragmentManager().setFragmentResult(getString(R.string.picker_selection), bundle);
  }
}
