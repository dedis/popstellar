package com.github.dedis.popstellar.ui.detail.event.eventlist;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.testutils.*;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListAdapter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

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
  public ActivityScenarioRule<LaoActivity> activityScenarioRule =
      new ActivityScenarioRule<>(
          IntentUtils.createIntent(
              LaoActivity.class, new BundleBuilder().putString(laoIdExtra(), LAO_ID).build()));

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
              LaoViewModel eventsViewModel = LaoActivity.obtainViewModel(activity);
              ref.set(new EventListAdapter(eventsViewModel, events, activity));
            });

    return ref.get();
  }
}
