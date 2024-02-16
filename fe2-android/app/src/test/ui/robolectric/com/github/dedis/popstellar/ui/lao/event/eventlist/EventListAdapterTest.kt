package com.github.dedis.popstellar.ui.lao.event.eventlist

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.RollCall.Companion.closeRollCall
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.model.objects.event.Event
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.IntentUtils
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.security.GeneralSecurityException
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.internal.util.collections.Sets
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EventListAdapterTest {
  private val ROLL_CALL =
    RollCall(
      LAO.id,
      LAO.id,
      "ROLL_CALL_TITLE",
      0L,
      1L,
      2L,
      EventState.CREATED,
      HashSet(),
      "not lausanne",
      "no"
    )
  private val ROLL_CALL2 =
    RollCall("12345", "12345", "Name", 2L, 3L, 4L, EventState.CREATED, HashSet(), "nowhere", "foo")

  private lateinit var events: Subject<Set<Event>>

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

        events = BehaviorSubject.create()
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
    val adapter = eventListAdapter
    events.onNext(emptySet())
    Assert.assertEquals(0, adapter.itemCount.toLong())
  }

  @Test
  fun updateAreDisplayed() {
    val adapter = eventListAdapter

    // Default values
    events.onNext(Sets.newSet<Event>(ROLL_CALL, ROLL_CALL2))
    Assert.assertEquals(EventListAdapter.TYPE_HEADER.toLong(), adapter.getItemViewType(0).toLong())
    Assert.assertEquals(EventListAdapter.TYPE_EVENT.toLong(), adapter.getItemViewType(1).toLong())
    Assert.assertEquals(EventListAdapter.TYPE_EVENT.toLong(), adapter.getItemViewType(2).toLong())

    // Close rollcall 1 should move it from current to past
    events.onNext(Sets.newSet<Event>(closeRollCall(ROLL_CALL), ROLL_CALL2))
    Assert.assertEquals(EventListAdapter.TYPE_HEADER.toLong(), adapter.getItemViewType(0).toLong())
    Assert.assertEquals(EventListAdapter.TYPE_EVENT.toLong(), adapter.getItemViewType(1).toLong())
    Assert.assertEquals(EventListAdapter.TYPE_HEADER.toLong(), adapter.getItemViewType(2).toLong())
    Assert.assertEquals(EventListAdapter.TYPE_EVENT.toLong(), adapter.getItemViewType(3).toLong())
  }

  private val eventListAdapter: EventListAdapter
    get() {
      val ref = AtomicReference<EventListAdapter>()
      activityScenarioRule.scenario.onActivity { activity: LaoActivity ->
        val eventsViewModel = obtainViewModel(activity)
        ref.set(EventListAdapter(eventsViewModel, events, activity))
      }
      return ref.get()
    }

  companion object {
    private val LAO = Lao("LAO", Base64DataUtils.generatePublicKey(), 10223421)
    private val LAO_ID = LAO.id
  }
}
