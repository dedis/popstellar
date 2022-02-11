package com.github.dedis.popstellar.ui.pages.detail.event;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import org.hamcrest.Matchers;

public class EventCreationPageObject {

  public static ViewInteraction startDateView() {
    return onView(withId(R.id.start_date_edit_text));
  }

  public static ViewInteraction endDateView() {
    return onView(withId(R.id.end_date_edit_text));
  }

  public static ViewInteraction datePicker() {
    return onView(withClassName(Matchers.equalTo(DatePicker.class.getName())));
  }

  public static ViewInteraction startTimeView() {
    return onView(withId(R.id.start_time_edit_text));
  }

  public static ViewInteraction endTimeView() {
    return onView(withId(R.id.end_time_edit_text));
  }

  public static ViewInteraction timePicker() {
    return onView(withClassName(Matchers.equalTo(TimePicker.class.getName())));
  }

  public static ViewInteraction pickerAcceptButton() {
    return onView(withId(android.R.id.button1));
  }
}
