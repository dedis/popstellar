package com.github.dedis.popstellar.ui.socialmedia;

import android.content.Intent;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.utility.Constants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

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

  private final Intent intent =
      new Intent(ApplicationProvider.getApplicationContext(), SocialMediaActivity.class)
          .putExtra(Constants.LAO_ID_EXTRA, LAO_ID);

  private final ActivityScenarioRule<SocialMediaActivity> activityScenarioRule =
      new ActivityScenarioRule<>(intent);

  private final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule public final RuleChain chain = RuleChain.outerRule(hiltRule).around(activityScenarioRule);

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
                  SocialMediaActivity.obtainViewModel(activity);
              ChirpListAdapter chirpListAdapter =
                  createChirpListAdapter(activity, socialMediaViewModel, new ArrayList<>());
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
                  SocialMediaActivity.obtainViewModel(activity);
              ChirpListAdapter chirpListAdapter =
                  createChirpListAdapter(activity, socialMediaViewModel, null);
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
                  SocialMediaActivity.obtainViewModel(activity);
              ChirpListAdapter chirpListAdapter =
                  createChirpListAdapter(activity, socialMediaViewModel, chirps);
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
                  SocialMediaActivity.obtainViewModel(activity);
              ChirpListAdapter chirpListAdapter =
                  createChirpListAdapter(activity, socialMediaViewModel, chirps);
              assertEquals(0, chirpListAdapter.getItemId(0));
              assertEquals(1, chirpListAdapter.getItemId(1));
            });
  }

  private List<Chirp> createChirpList() {
    return new ArrayList<>(
        Arrays.asList(ChirpListAdapterTest.CHIRP_1, ChirpListAdapterTest.CHIRP_2));
  }

  private ChirpListAdapter createChirpListAdapter(
      FragmentActivity activity, SocialMediaViewModel socialMediaViewModel, List<Chirp> chirps) {
    ChirpListAdapter adapter = new ChirpListAdapter(activity, socialMediaViewModel);
    adapter.replaceList(chirps);
    return adapter;
  }
}
