package com.github.dedis.popstellar.ui.lao.socialmedia;

import android.view.View;
import android.widget.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.FragmentActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.repository.SocialMediaRepository;
import com.github.dedis.popstellar.testutils.BundleBuilder;
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule;
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoTestRule;

import java.time.Instant;
import java.util.*;

import javax.inject.Inject;

import dagger.hilt.android.testing.*;

import static android.text.format.DateUtils.getRelativeTimeSpanString;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

  private static final MessageID MESSAGE_ID_1 = generateMessageID();
  private static final MessageID MESSAGE_ID_2 = generateMessageID();

  private static final String TEXT_1 = "text1";
  private static final String TEXT_2 = "text2";
  private static final long TIMESTAMP_1 = 1632204910;
  private static final long TIMESTAMP_2 = 1632204900;

  private static final Chirp CHIRP_1 =
      new Chirp(MESSAGE_ID_1, SENDER_1, TEXT_1, TIMESTAMP_1, new MessageID(""));
  private static final Chirp CHIRP_2 =
      new Chirp(MESSAGE_ID_2, SENDER_2, TEXT_2, TIMESTAMP_2, new MessageID("")).deleted();

  private final RollCall ROLL_CALL =
      new RollCall(
          LAO_ID,
          LAO_ID,
          "",
          CREATION_TIME,
          TIMESTAMP_1,
          TIMESTAMP_2,
          EventState.CLOSED,
          new HashSet<>(),
          "",
          "");

  private final MessageID REACTION_ID = generateMessageID();
  private static final long TIMESTAMP = Instant.now().getEpochSecond();
  private final Reaction REACTION =
      new Reaction(
          REACTION_ID,
          SENDER_1,
          Reaction.ReactionEmoji.UPVOTE.getCode(),
          CHIRP_1.getId(),
          TIMESTAMP);

  @Inject SocialMediaRepository socialMediaRepository;
  @Inject RollCallRepository rollCallRepository;
  @BindValue @Mock KeyManager keyManager;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Rule(order = 0)
  public final MockitoTestRule mockitoRule = MockitoJUnit.testRule(this);

  @Rule(order = 1)
  public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 2)
  public final ExternalResource setupRule =
      new ExternalResource() {
        @Override
        protected void before() throws KeyException {
          hiltRule.inject();

          rollCallRepository.updateRollCall(LAO_ID, ROLL_CALL);
          socialMediaRepository.addChirp(LAO_ID, CHIRP_1);
          socialMediaRepository.addChirp(LAO_ID, CHIRP_2);
          socialMediaRepository.addReaction(LAO_ID, REACTION);

          when(keyManager.getValidPoPToken(anyString(), any(RollCall.class)))
              .thenReturn((PoPToken) SENDER_KEY_1);
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

  @Test
  public void getViewMainElementsTest() {
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

              // Use a non null ViewGroup to inflate the card
              LinearLayout layout = new LinearLayout(activity.getApplicationContext());
              TextView parent = new TextView(activity.getApplicationContext());
              parent.setText("Mock Title");
              layout.addView(parent);

              // Get the view for the first chirp in the list.
              View view1 = chirpListAdapter.getView(0, null, layout);
              assertNotNull(view1);

              // Check the text is matching correctly
              TextView textView = view1.findViewById(R.id.social_media_text);
              assertNotNull(textView);
              assertEquals(TEXT_1, textView.getText().toString());

              // Check the user is matching correctly
              TextView user = view1.findViewById(R.id.social_media_username);
              assertNotNull(user);
              assertEquals(SENDER_1.getEncoded(), user.getText().toString());

              // Check the time is matching correctly
              TextView time = view1.findViewById(R.id.social_media_time);
              assertNotNull(time);
              assertEquals(
                  getRelativeTimeSpanString(TIMESTAMP_1 * 1000), time.getText().toString());

              // Ensure that the buttons are visible
              LinearLayout buttons = view1.findViewById(R.id.chirp_card_buttons);
              assertNotNull(buttons);
              assertEquals(View.VISIBLE, buttons.getVisibility());

              // Assert that the bin is visible
              ImageButton bin = view1.findViewById(R.id.delete_chirp_button);
              assertNotNull(bin);
              assertEquals(View.VISIBLE, bin.getVisibility());

              // Get the view for the second chirp in the list.
              View view2 = chirpListAdapter.getView(1, null, layout);
              assertNotNull(view2);

              // Assert text is deleted
              TextView textView2 = view2.findViewById(R.id.social_media_text);
              assertNotNull(textView2);
              assertEquals(
                  activity.getApplicationContext().getString(R.string.deleted_chirp_2),
                  textView2.getText().toString());

              // Assert that the bin is not visible
              ImageButton bin2 = view2.findViewById(R.id.delete_chirp_button);
              assertNotNull(bin2);
              assertEquals(View.GONE, bin2.getVisibility());
            });
  }

  @Test
  public void getViewReactionTest() {
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

              // Use a non null ViewGroup to inflate the card
              LinearLayout layout = new LinearLayout(activity.getApplicationContext());
              TextView parent = new TextView(activity.getApplicationContext());
              parent.setText("Mock Title");
              layout.addView(parent);

              // Get the view for the first chirp in the list.
              View view1 = chirpListAdapter.getView(0, null, layout);
              assertNotNull(view1);

              TextView upvoteCount = view1.findViewById(R.id.upvote_counter);
              assertNotNull(upvoteCount);
              ImageButton upvoteButton = view1.findViewById(R.id.upvote_button);
              assertNotNull(upvoteButton);
              assertTrue(upvoteButton.isSelected());
            });
  }

  private static List<Chirp> createChirpList() {
    return new ArrayList<>(Arrays.asList(CHIRP_1, CHIRP_2));
  }

  private static ChirpListAdapter createChirpListAdapter(
      FragmentActivity activity,
      LaoViewModel viewModel,
      SocialMediaViewModel socialMediaViewModel,
      List<Chirp> chirps) {
    ChirpListAdapter adapter = new ChirpListAdapter(activity, socialMediaViewModel, viewModel);
    adapter.replaceList(chirps);
    return adapter;
  }
}
