package com.github.dedis.popstellar.ui.pages.detail;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

public class LaoDetailFragmentPageObject {

  public static ViewInteraction titleTextView() {
    return onView(withId(R.id.lao_detail_title));
  }

  public static ViewInteraction qrCodeIcon() {
    return onView(withId(R.id.qr_code_icon));
  }

  public static ViewInteraction qrCodeLayout() {
    return onView(withId(R.id.lao_detail_qr_layout));
  }

  public static ViewInteraction connectQrCode() {
    return onView(withId(R.id.channel_qr_code));
  }

  public static ViewInteraction addEventButton() {
    return onView(withId(R.id.add_event));
  }

  public static ViewInteraction addElectionButton() {
    return onView(withId(R.id.add_election));
  }

  public static ViewInteraction addElectionText() {
    return onView(withId(R.id.add_election_text));
  }

  public static ViewInteraction addRollCallButton() {
    return onView(withId(R.id.add_roll_call));
  }

  public static ViewInteraction addRollCallText() {
    return onView(withId(R.id.add_roll_call_text));
  }
  public static ViewInteraction eventList() {
    return onView(withId(R.id.event_list));
  }
}
