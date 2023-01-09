package com.github.dedis.popstellar.ui.detail;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.testutils.*;
import com.github.dedis.popstellar.ui.detail.event.EventListAdapter;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

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

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.*;
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

  @Mock LaoDetailViewModel laoDetailViewModel;
  @BindValue @Mock Wallet wallet;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() throws GeneralSecurityException, UnknownLaoException {
          hiltRule.inject();

          events = BehaviorSubject.create();
          when(laoDetailViewModel.getEvents()).thenReturn(events);
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
    events.onNext(Collections.emptySet());

    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
              EventListAdapter adapter = new EventListAdapter(laoDetailViewModel, activity);
              assertEquals(3, adapter.getItemCount());
            });
  }

  @Test
  public void updateAreDisplayed() {
    // Default values
    events.onNext(Sets.newSet(ROLL_CALL, ROLL_CALL2));

    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
              EventListAdapter adapter = new EventListAdapter(laoDetailViewModel, activity);

              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(0));
              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(1));
              assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(2));
              assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(3));
              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(4));

              // Open rollcall 1
              events.onNext(Sets.newSet(RollCall.openRollCall(ROLL_CALL), ROLL_CALL2));

              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(0));
              assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(1));
              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(2));
              assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(3));
              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(4));
            });
  }
}
