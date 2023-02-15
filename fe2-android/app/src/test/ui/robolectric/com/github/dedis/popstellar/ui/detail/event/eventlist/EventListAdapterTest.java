package com.github.dedis.popstellar.ui.detail;

import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.fragmentToOpenExtra;
import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.laoDetailValue;
import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.laoIdExtra;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.IntentUtils;
import com.github.dedis.popstellar.ui.detail.event.eventlist.EventListAdapter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class EventListAdapterTest {

  private static final Lao LAO = new Lao("LAO", Base64DataUtils.generatePublicKey(), 10223421);
  private static final String LAO_ID = LAO.getId();
  private final RollCall ROLL_CALL =
      new RollCall(
          LAO.getId(),
          LAO.getId(),
          "ROLL_CALL_TITLE",
          0L,
          1L,
          2L,
          EventState.CREATED,
          new HashSet<>(),
          "not lausanne",
          "no");

  private final RollCall ROLL_CALL2 =
      new RollCall(
          "12345",
          "12345",
          "Name",
          2L,
          3L,
          4L,
          EventState.CREATED,
          new HashSet<>(),
          "nowhere",
          "foo");

  private Subject<Set<Event>> events;

  @BindValue @Mock Wallet wallet;

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() throws GeneralSecurityException {
          hiltRule.inject();

          events = BehaviorSubject.create();
          when(wallet.exportSeed())
              .thenReturn(
                  new String[] {
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
                  });
        }
      };

  @Rule(order = 3)
  public ActivityScenarioRule<LaoDetailActivity> activityScenarioRule =
      new ActivityScenarioRule<>(
          IntentUtils.createIntent(
              LaoDetailActivity.class,
              new BundleBuilder()
                  .putString(laoIdExtra(), LAO_ID)
                  .putString(fragmentToOpenExtra(), laoDetailValue())
                  .build()));

  @Test
  public void emptyAdapterTest() {
    EventListAdapter adapter = getEventListAdapter();

    events.onNext(Collections.emptySet());
    assertEquals(0, adapter.getItemCount());
  }

  @Test
  public void updateAreDisplayed() {
    EventListAdapter adapter = getEventListAdapter();

    // Default values
    events.onNext(Sets.newSet(ROLL_CALL, ROLL_CALL2));
    assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(0));
    assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(1));
    assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(2));

    // Close rollcall 1 should move it from current to past
    events.onNext(Sets.newSet(RollCall.closeRollCall(ROLL_CALL), ROLL_CALL2));

    assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(0));
    assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(1));
    assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(2));
    assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(3));
  }

  private EventListAdapter getEventListAdapter() {
    AtomicReference<EventListAdapter> ref = new AtomicReference<>();

    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel viewModel = LaoDetailActivity.obtainViewModel(activity);
              ref.set(new EventListAdapter(viewModel, events, activity));
            });

    return ref.get();
  }
}
