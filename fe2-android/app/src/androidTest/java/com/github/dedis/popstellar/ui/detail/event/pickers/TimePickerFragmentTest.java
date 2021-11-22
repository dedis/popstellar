package com.github.dedis.popstellar.ui.detail.event.pickers;

import static com.github.dedis.popstellar.pages.detail.event.pickers.TimePickerPageObject.getBundleResponseKey;
import static com.github.dedis.popstellar.pages.detail.event.pickers.TimePickerPageObject.getRequestKey;
import static org.junit.Assert.assertEquals;

import android.widget.TimePicker;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.github.dedis.popstellar.testutils.FragmentScenarioRule;
import com.github.dedis.popstellar.testutils.ResultReceiver;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4ClassRunner.class)
public class TimePickerFragmentTest {

  private static final int HOURS = 8;
  private static final int MINUTES = 37;

  @Rule
  public final FragmentScenarioRule<TimePickerFragment> fragmentRule =
      FragmentScenarioRule.launchInContainer(TimePickerFragment.class);

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
