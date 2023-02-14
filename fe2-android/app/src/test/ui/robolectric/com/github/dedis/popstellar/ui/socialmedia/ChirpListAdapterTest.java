package com.github.dedis.popstellar.ui.socialmedia;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.FragmentActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.Constants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.util.*;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static org.junit.Assert.assertEquals;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class ChirpListAdapterTest {
  private static final long CREATION_TIME = 1631280815;
  private static final String LAO_NAME = "laoName";

  private static final KeyPair SENDER_KEY_1 = generatePoPToken();
  private static final KeyPair SENDER_KEY_2 = generatePoPToken();

  private static final PublicKey SENDER_1 = SENDER_KEY_1.getPublicKey();
  private static final PublicKey SENDER_2 = SENDER_KEY_2.getPublicKey();

  private static final String LAO_ID = Lao.generateLaoId(SENDER_1, CREATION_TIME, LAO_NAME);

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
        }
      };

  @Rule(order = 3)
  public ActivityFragmentScenarioRule<LaoActivity, ChirpListFragment> activityScenarioRule =
      ActivityFragmentScenarioRule.launchIn(
          LaoActivity.class,
          new BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO_ID).build(),
          LaoActivityPageObject.containerId(),
          ChirpListFragment.class,
          ChirpListFragment::new);

  private static final MessageID MESSAGE_ID_1 = generateMessageID();
  private static final MessageID MESSAGE_ID_2 = generateMessageID();

  private static final Channel CHIRP_CHANNEL_1 =
      Channel.getLaoChannel(LAO_ID).subChannel("social").subChannel(SENDER_1.getEncoded());
  private static final Channel CHIRP_CHANNEL_2 =
      Channel.getLaoChannel(LAO_ID).subChannel("social").subChannel(SENDER_2.getEncoded());
  private static final String TEXT_1 = "text1";
  private static final String TEXT_2 = "text2";
  private static final long TIMESTAMP_1 = 1632204910;
  private static final long TIMESTAMP_2 = 1632204900;

  private static final Chirp CHIRP_1 =
      new Chirp(MESSAGE_ID_1, SENDER_1, TEXT_1, TIMESTAMP_1, new MessageID(""));
  private static final Chirp CHIRP_2 =
      new Chirp(MESSAGE_ID_2, SENDER_2, TEXT_2, TIMESTAMP_2, new MessageID("")).deleted();

  @Test
  public void replaceListTest() {
    List<Chirp> chirps = createChirpList();

    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              SocialMediaViewModel socialMediaViewModel =
                  LaoActivity.obtainSocialMediaViewModel(activity, LAO_ID);
              LaoViewModel viewModel = LaoActivity.obtainViewModel(activity);
              ChirpListAdapter chirpListAdapter =
                  createChirpListAdapter(
                      activity, viewModel, socialMediaViewModel, new ArrayList<>());
              chirpListAdapter.replaceList(chirps);
              assertEquals(chirps.size(), chirpListAdapter.getCount());
            });
  }

  @Test
  public void getCountTest() {
    List<Chirp> chirps = createChirpList();

    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              SocialMediaViewModel socialMediaViewModel =
                  LaoActivity.obtainSocialMediaViewModel(activity, LAO_ID);
              LaoViewModel viewModel = LaoActivity.obtainViewModel(activity);
              ChirpListAdapter chirpListAdapter =
                  createChirpListAdapter(activity, viewModel, socialMediaViewModel, null);
              assertEquals(0, chirpListAdapter.getCount());
              chirpListAdapter.replaceList(chirps);
              assertEquals(chirps.size(), chirpListAdapter.getCount());
            });
  }

  @Test
  public void getItemTest() {
    List<Chirp> chirps = createChirpList();

    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              SocialMediaViewModel socialMediaViewModel =
                  LaoActivity.obtainSocialMediaViewModel(activity, LAO_ID);
              LaoViewModel viewModel = LaoActivity.obtainViewModel(activity);
              ChirpListAdapter chirpListAdapter =
                  createChirpListAdapter(activity, viewModel, socialMediaViewModel, chirps);
              assertEquals(CHIRP_1, chirpListAdapter.getItem(0));
              assertEquals(CHIRP_2, chirpListAdapter.getItem(1));
            });
  }

  @Test
  public void getItemIdTest() {
    List<Chirp> chirps = createChirpList();

    activityScenarioRule
        .getScenario()
        .onActivity(
            activity -> {
              SocialMediaViewModel socialMediaViewModel =
                  LaoActivity.obtainSocialMediaViewModel(activity, LAO_ID);
              LaoViewModel viewModel = LaoActivity.obtainViewModel(activity);
              ChirpListAdapter chirpListAdapter =
                  createChirpListAdapter(activity, viewModel, socialMediaViewModel, chirps);
              assertEquals(0, chirpListAdapter.getItemId(0));
              assertEquals(1, chirpListAdapter.getItemId(1));
            });
  }

  private List<Chirp> createChirpList() {
    return new ArrayList<>(
        Arrays.asList(ChirpListAdapterTest.CHIRP_1, ChirpListAdapterTest.CHIRP_2));
  }

  private ChirpListAdapter createChirpListAdapter(
      FragmentActivity activity,
      LaoViewModel viewModel,
      SocialMediaViewModel socialMediaViewModel,
      List<Chirp> chirps) {
    ChirpListAdapter adapter = new ChirpListAdapter(activity, socialMediaViewModel, viewModel);
    adapter.replaceList(chirps);
    return adapter;
  }
}
