package com.github.dedis.popstellar.ui.detail.event.pickers;

import android.widget.DatePicker;

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

import static com.github.dedis.popstellar.testutils.pages.detail.event.pickers.DatePickerPageObject.getBundleResponseKey;
import static com.github.dedis.popstellar.testutils.pages.detail.event.pickers.DatePickerPageObject.getRequestKey;
import static org.junit.Assert.assertEquals;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class DatePickerFragmentTest {

  private static final int YEAR = 2022;
  private static final int MONTH_OF_YEAR = 10;
  private static final int DAY_OF_MONTH = 10;

  private final FragmentScenarioRule<DatePickerFragment> fragmentRule =
      FragmentScenarioRule.launch(DatePickerFragment.class);

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
        .onFragment(
            f -> f.onDateSet(new DatePicker(f.getContext()), YEAR, MONTH_OF_YEAR, DAY_OF_MONTH));

    Calendar response = receiver.get(500);
    assertEquals(YEAR, response.get(Calendar.YEAR));
    assertEquals(MONTH_OF_YEAR, response.get(Calendar.MONTH));
    assertEquals(DAY_OF_MONTH, response.get(Calendar.DAY_OF_MONTH));
  }
}
