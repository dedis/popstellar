package com.github.dedis.popstellar.ui.lao;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsEntity;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.testutils.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject.laoIdExtra;
import static org.junit.Assert.assertEquals;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LaoActivityTest {

  private static final String LAO_NAME = "LAO";
  private static final KeyPair KEY_PAIR = Base64DataUtils.generateKeyPair();
  private static final PublicKey PK = KEY_PAIR.getPublicKey();
  private static final Lao LAO = new Lao(LAO_NAME, PK, Instant.now().getEpochSecond());

  private static final String URL = "url";
  private static final Set<Channel> CHANNELS = new HashSet<>();

  static {
    CHANNELS.add(Channel.getLaoChannel(LAO.getId()));
    CHANNELS.add(Channel.getLaoChannel(LAO.getId()).subChannel("random"));
  }

  @Inject AppDatabase appDatabase;
  @Inject GlobalNetworkManager globalNetworkManager;

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
          SubscriptionsEntity subscriptionsEntity =
              new SubscriptionsEntity(LAO.getId(), URL, CHANNELS);
          appDatabase.subscriptionsDao().insert(subscriptionsEntity).test().awaitTerminalEvent();
        }
      };

  @Rule(order = 3)
  public ActivityScenarioRule<LaoActivity> activityScenarioRule =
      new ActivityScenarioRule<>(
          IntentUtils.createIntent(
              LaoActivity.class, new BundleBuilder().putString(laoIdExtra(), LAO.getId()).build()));

  @Test
  public void restoreConnectionsTest() {
    assertEquals(CHANNELS, globalNetworkManager.getMessageSender().getSubscriptions());
  }
}
