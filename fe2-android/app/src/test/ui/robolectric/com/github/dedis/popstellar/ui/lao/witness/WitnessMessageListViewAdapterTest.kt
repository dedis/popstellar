package com.github.dedis.popstellar.ui.lao.witness

import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.IntentUtils
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WitnessMessageListViewAdapterTest {
  private lateinit var adapter: WitnessMessageListViewAdapter

  @BindValue @Mock lateinit var wallet: Wallet

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      @Throws(GeneralSecurityException::class)
      override fun before() {
        hiltRule.inject()

        Mockito.`when`(wallet.exportSeed())
          .thenReturn(
            arrayOf(
              "jar",
              "together",
              "minor",
              "alley",
              "glow",
              "hybrid",
              "village",
              "creek",
              "meadow",
              "atom",
              "travel",
              "bracket"
            )
          )
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule =
    ActivityScenarioRule<LaoActivity>(
      IntentUtils.createIntent(
        LaoActivity::class.java,
        BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build()
      )
    )

  @Test
  fun emptyAdapterTest() {
    adapter = WitnessMessageListViewAdapter(null, fragmentActivity)
    Assert.assertEquals(0, adapter.count.toLong())
  }

  @Test
  fun oneElementAdapterTest() {
    adapter = WitnessMessageListViewAdapter(WITNESS_MESSAGES1, fragmentActivity)

    Assert.assertEquals(1, adapter.count.toLong())
    Assert.assertEquals(WITNESS_MESSAGE1, adapter.getItem(0))
    Assert.assertEquals(0, adapter.getItemId(0))
  }

  @Test
  fun replaceListTest() {
    adapter = WitnessMessageListViewAdapter(WITNESS_MESSAGES1, fragmentActivity)
    adapter.replaceList(WITNESS_MESSAGES2)

    Assert.assertEquals(1, adapter.count.toLong())
    Assert.assertEquals(WITNESS_MESSAGE2, adapter.getItem(0))
  }

  private val fragmentActivity: FragmentActivity
    get() {
      val ref = AtomicReference<FragmentActivity>()
      activityScenarioRule.scenario.onActivity { activity: LaoActivity -> ref.set(activity) }
      return ref.get()
    }

  companion object {
    private val LAO = Lao("LAO", Base64DataUtils.generatePublicKey(), Instant.now().epochSecond)
    private val LAO_ID = LAO.id
    private val MESSAGE_ID1 = Base64DataUtils.generateMessageID()
    private val MESSAGE_ID2 = Base64DataUtils.generateMessageID()
    private val WITNESS_MESSAGE1 = WitnessMessage(MESSAGE_ID1)
    private val WITNESS_MESSAGE2 = WitnessMessage(MESSAGE_ID2)
    private val WITNESS_MESSAGES1 = listOf(WITNESS_MESSAGE1)
    private val WITNESS_MESSAGES2 = listOf(WITNESS_MESSAGE2)
  }
}
