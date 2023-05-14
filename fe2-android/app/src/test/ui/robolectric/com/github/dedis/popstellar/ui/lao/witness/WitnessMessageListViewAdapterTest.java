package com.github.dedis.popstellar.ui.lao.witness;

import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import androidx.fragment.app.FragmentActivity;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.testutils.*;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import dagger.hilt.android.testing.*;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class WitnessMessageListViewAdapterTest {

  private static final Lao LAO =
      new Lao("LAO", Base64DataUtils.generatePublicKey(), Instant.now().getEpochSecond());
  private static final String LAO_ID = LAO.getId();
  private static final MessageID MESSAGE_ID1 = Base64DataUtils.generateMessageID();
  private static final MessageID MESSAGE_ID2 = Base64DataUtils.generateMessageID();
  private static final WitnessMessage WITNESS_MESSAGE1 = new WitnessMessage(MESSAGE_ID1);
  private static final WitnessMessage WITNESS_MESSAGE2 = new WitnessMessage(MESSAGE_ID2);

  private static final List<WitnessMessage> WITNESS_MESSAGES1 =
      Collections.singletonList(WITNESS_MESSAGE1);
  private static final List<WitnessMessage> WITNESS_MESSAGES2 =
      Collections.singletonList(WITNESS_MESSAGE2);

  WitnessMessageListViewAdapter adapter;

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
    adapter = new WitnessMessageListViewAdapter(null, getFragmentActivity());

    assertEquals(0, adapter.getCount());
  }

  @Test
  public void oneElementAdapterTest() {
    adapter = new WitnessMessageListViewAdapter(WITNESS_MESSAGES1, getFragmentActivity());

    assertEquals(1, adapter.getCount());
    assertEquals(WITNESS_MESSAGE1, adapter.getItem(0));
    assertEquals(0, adapter.getItemId(0));
  }

  @Test
  public void replaceListTest() {
    adapter = new WitnessMessageListViewAdapter(WITNESS_MESSAGES1, getFragmentActivity());
    adapter.replaceList(WITNESS_MESSAGES2);

    assertEquals(1, adapter.getCount());
    assertEquals(WITNESS_MESSAGE2, adapter.getItem(0));
  }

  private FragmentActivity getFragmentActivity() {
    AtomicReference<FragmentActivity> ref = new AtomicReference<>();
    activityScenarioRule.getScenario().onActivity(activity -> ref.set(activity));

    return ref.get();
  }
}
