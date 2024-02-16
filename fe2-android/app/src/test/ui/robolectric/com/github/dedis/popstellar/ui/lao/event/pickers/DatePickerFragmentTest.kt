package com.github.dedis.popstellar.ui.lao.event.pickers

import android.widget.DatePicker
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.testutils.ResultReceiver
import com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.event.pickers.DatePickerPageObject
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Calendar
import java.util.concurrent.TimeoutException
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DatePickerFragmentTest {

  private val fragmentRule = FragmentScenarioRule.launch(DatePickerFragment::class.java)

  @JvmField
  @Rule
  val chain: RuleChain = RuleChain.outerRule(HiltAndroidRule(this)).around(fragmentRule)

  @Test
  @Throws(TimeoutException::class, InterruptedException::class)
  fun choosingADateReturnTheCorrectValue() {
    val receiver =
      ResultReceiver.createFakeListener<Calendar>(
        fragmentRule.scenario,
        DatePickerPageObject.getRequestKey(),
        DatePickerPageObject.getBundleResponseKey()
      )
    fragmentRule.scenario.onFragment { f: DatePickerFragment ->
      f.onDateSet(DatePicker(f.context), YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    }
    val response = receiver[500]
    Assert.assertEquals(YEAR.toLong(), response[Calendar.YEAR].toLong())
    Assert.assertEquals(MONTH_OF_YEAR.toLong(), response[Calendar.MONTH].toLong())
    Assert.assertEquals(DAY_OF_MONTH.toLong(), response[Calendar.DAY_OF_MONTH].toLong())
  }

  companion object {
    private const val YEAR = 2022
    private const val MONTH_OF_YEAR = 10
    private const val DAY_OF_MONTH = 10
  }
}
