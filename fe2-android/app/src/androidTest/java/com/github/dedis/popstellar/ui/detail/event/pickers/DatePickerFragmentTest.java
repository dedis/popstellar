package com.github.dedis.popstellar.ui.detail.event.pickers;

import static com.github.dedis.popstellar.pages.detail.event.pickers.DatePickerPageObject.getBundleResponseKey;
import static com.github.dedis.popstellar.pages.detail.event.pickers.DatePickerPageObject.getRequestKey;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import android.widget.DatePicker;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.github.dedis.popstellar.testutils.FragmentScenarioRule;
import com.github.dedis.popstellar.testutils.ResultReceiver;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4ClassRunner.class)
public class DatePickerFragmentTest {

  private static final int YEAR = 2022;
  private static final int MONTH_OF_YEAR = 10;
  private static final int DAY_OF_MONTH = 10;

  @Rule
  public final FragmentScenarioRule<DatePickerFragment> fragmentRule =
      FragmentScenarioRule.launchInContainer(DatePickerFragment.class);

  @Test
  public void choosingADateReturnTheCorrectValue() throws TimeoutException, InterruptedException {
    ResultReceiver<Calendar> receiver =
        ResultReceiver.createFakeListener(
            fragmentRule.getScenario(), getRequestKey(), getBundleResponseKey());

    fragmentRule
        .getScenario()
        .onFragment(f -> f.onDateSet(mock(DatePicker.class), YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));

    Calendar response = receiver.get(500);
    assertEquals(YEAR, response.get(Calendar.YEAR));
    assertEquals(MONTH_OF_YEAR, response.get(Calendar.MONTH));
    assertEquals(DAY_OF_MONTH, response.get(Calendar.DAY_OF_MONTH));
  }
}
