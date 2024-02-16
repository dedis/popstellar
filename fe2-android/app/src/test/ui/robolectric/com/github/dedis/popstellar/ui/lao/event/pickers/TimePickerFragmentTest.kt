package com.github.dedis.popstellar.ui.lao.event.pickers

import android.widget.TimePicker
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.testutils.ResultReceiver
import com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.event.pickers.TimePickerPageObject
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
class TimePickerFragmentTest {

  private val fragmentRule = FragmentScenarioRule.launch(TimePickerFragment::class.java)

  @JvmField
  @Rule
  val chain: RuleChain = RuleChain.outerRule(HiltAndroidRule(this)).around(fragmentRule)

  @Test
  @Throws(TimeoutException::class, InterruptedException::class)
  fun choosingADateReturnTheCorrectValue() {
    val receiver =
      ResultReceiver.createFakeListener<Calendar>(
        fragmentRule.scenario,
        TimePickerPageObject.getRequestKey(),
        TimePickerPageObject.getBundleResponseKey()
      )
    fragmentRule.scenario.onFragment { f: TimePickerFragment ->
      f.onTimeSet(TimePicker(f.context), HOURS, MINUTES)
    }
    val response = receiver[500]
    Assert.assertEquals(HOURS.toLong(), response[Calendar.HOUR_OF_DAY].toLong())
    Assert.assertEquals(MINUTES.toLong(), response[Calendar.MINUTE].toLong())
  }

  companion object {
    private const val HOURS = 8
    private const val MINUTES = 37
  }
}
