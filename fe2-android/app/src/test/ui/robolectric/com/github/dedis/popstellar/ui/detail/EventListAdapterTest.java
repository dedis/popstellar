package com.github.dedis.popstellar.ui.detail;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.testutils.*;
import com.github.dedis.popstellar.ui.detail.event.EventListAdapter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.*;

import dagger.hilt.android.testing.*;
import io.reactivex.subjects.BehaviorSubject;

import static com.github.dedis.popstellar.testutils.pages.detail.LaoDetailActivityPageObject.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class EventListAdapterTest {
  private static final Lao LAO = new Lao("LAO", Base64DataUtils.generatePublicKey(), 10223421);
  private static final String LAO_ID = LAO.getId();
  private static final RollCall ROLL_CALL = new RollCall("12345");
  private static final RollCall ROLL_CALL2 = new RollCall("54321");

  @BindValue @Mock LAORepository repository;
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
        protected void before() {
          hiltRule.inject();
          when(repository.getLaoObservable(anyString()))
              .thenReturn(BehaviorSubject.createDefault(new LaoView(LAO)));

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
    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
              EventListAdapter adapter =
                  new EventListAdapter(new ArrayList<>(), laoDetailViewModel, activity);
              assertEquals(3, adapter.getItemCount());
            });
  }

  @Test
  public void viewTypeAdapterTest() {
    ROLL_CALL.setState(EventState.OPENED);
    ROLL_CALL2.setState(EventState.CREATED);
    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
              EventListAdapter adapter =
                  new EventListAdapter(
                      Arrays.asList(ROLL_CALL, ROLL_CALL2), laoDetailViewModel, activity);
              assertEquals(5, adapter.getItemCount());
              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(0));
              assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(1));
              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(2));
              assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(3));
              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(4));
            });
  }

  @Test
  public void replaceListTest() {
    ROLL_CALL.setState(EventState.OPENED);
    ROLL_CALL2.setState(EventState.CREATED);
    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
              EventListAdapter adapter =
                  new EventListAdapter(
                      Arrays.asList(ROLL_CALL, ROLL_CALL2), laoDetailViewModel, activity);
              adapter.replaceList(Collections.singletonList(ROLL_CALL2));
              assertEquals(4, adapter.getItemCount());
              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(0));
              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(1));
              assertEquals(EventListAdapter.TYPE_EVENT, adapter.getItemViewType(2));
              assertEquals(EventListAdapter.TYPE_HEADER, adapter.getItemViewType(3));
            });
  }
}
