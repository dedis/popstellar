package com.github.dedis.popstellar.ui.detail.event.pickers;

import android.widget.TimePicker;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.testutils.ResultReceiver;
import com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.concurrent.TimeoutException;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static com.github.dedis.popstellar.testutils.pages.detail.event.pickers.TimePickerPageObject.getBundleResponseKey;
import static com.github.dedis.popstellar.testutils.pages.detail.event.pickers.TimePickerPageObject.getRequestKey;
import static org.junit.Assert.assertEquals;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class TimePickerFragmentTest {

  private static final int HOURS = 8;
  private static final int MINUTES = 37;

  private final FragmentScenarioRule<TimePickerFragment> fragmentRule =
      FragmentScenarioRule.launch(TimePickerFragment.class);

  @Rule
  public final RuleChain chain =
      RuleChain.outerRule(new HiltAndroidRule(this)).around(fragmentRule);

  @Test
  public void choosingADateReturnTheCorrectValue() throws TimeoutException, InterruptedException {
    ResultReceiver<Calendar> receiver =
        ResultReceiver.createFakeListener(
            fragmentRule.getScenario(), getRequestKey(), getBundleResponseKey());

    fragmentRule
        .getScenario()
        .onFragment(f -> f.onTimeSet(new TimePicker(f.getContext()), HOURS, MINUTES));

    Calendar response = receiver.get(500);
    assertEquals(HOURS, response.get(Calendar.HOUR_OF_DAY));
    assertEquals(MINUTES, response.get(Calendar.MINUTE));
  }
}
