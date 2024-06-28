package com.github.dedis.popstellar.ui.lao.socialmedia

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Chirp
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.SocialMediaRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MessageSenderHelper
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainSocialMediaViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.Constants
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Observable
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule
import java.time.Instant
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChirpListAdapterTest {
  private val ROLL_CALL =
    RollCall(
      LAO_ID,
      LAO_ID,
      "",
      CREATION_TIME,
      TIMESTAMP_1,
      TIMESTAMP_2,
      EventState.CLOSED,
      LinkedHashSet(),
      "",
      "",
    )
  private val REACTION_ID = Base64DataUtils.generateMessageID()
  private val REACTION =
    Reaction(REACTION_ID, SENDER_1, Reaction.ReactionEmoji.UPVOTE.code, CHIRP_1.id, TIMESTAMP)
  private val REACTION2 =
    Reaction(
      Base64DataUtils.generateMessageID(),
      SENDER_2,
      Reaction.ReactionEmoji.DOWNVOTE.code,
      CHIRP_3.id,
      TIMESTAMP,
    )
  private val REACTION3 =
    Reaction(
      Base64DataUtils.generateMessageID(),
      SENDER_1,
      Reaction.ReactionEmoji.HEART.code,
      CHIRP_1.id,
      TIMESTAMP,
    )

  @Inject lateinit var socialMediaRepository: SocialMediaRepository

  @Inject lateinit var rollCallRepository: RollCallRepository

  @BindValue @Mock lateinit var networkManager: GlobalNetworkManager

  @BindValue @Mock lateinit var keyManager: KeyManager

  var messageSenderHelper = MessageSenderHelper()

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      @Throws(KeyException::class)
      override fun before() {
        hiltRule.inject()

        rollCallRepository.updateRollCall(LAO_ID, ROLL_CALL)
        socialMediaRepository.addChirp(LAO_ID, CHIRP_1)
        socialMediaRepository.addChirp(LAO_ID, CHIRP_2)
        socialMediaRepository.addReaction(LAO_ID, REACTION)
        Mockito.`when`(networkManager.messageSender).thenReturn(messageSenderHelper.mockedSender)
        messageSenderHelper.setupMock()
        Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER_KEY_1.publicKey)
        Mockito.`when`(
            keyManager.getValidPoPToken(ArgumentMatchers.anyString(), MockitoKotlinHelpers.any())
          )
          .thenReturn(SENDER_KEY_1 as PoPToken)
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, ChirpListFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      ChirpListFragment::class.java,
    ) {
      ChirpListFragment()
    }

  @Test
  fun replaceListTest() {
    val chirps = createChirpList()
    activityScenarioRule.scenario.onActivity { activity: LaoActivity ->
      val socialMediaViewModel = obtainSocialMediaViewModel(activity, LAO_ID)
      val viewModel = obtainViewModel(activity)
      val chirpListAdapter =
        createChirpListAdapter(activity, viewModel, socialMediaViewModel, ArrayList())
      chirpListAdapter.replaceList(chirps)

      Assert.assertEquals(chirps.size.toLong(), chirpListAdapter.count.toLong())
    }
  }

  @Test
  fun countTest() {
    val chirps = createChirpList()
    activityScenarioRule.scenario.onActivity { activity: LaoActivity ->
      val socialMediaViewModel = obtainSocialMediaViewModel(activity, LAO_ID)
      val viewModel = obtainViewModel(activity)
      val chirpListAdapter = createChirpListAdapter(activity, viewModel, socialMediaViewModel, null)

      Assert.assertEquals(0, chirpListAdapter.count.toLong())
      chirpListAdapter.replaceList(chirps)
      Assert.assertEquals(chirps.size.toLong(), chirpListAdapter.count.toLong())
    }
  }

  @Test
  fun itemTest() {
    val chirps = createChirpList()
    activityScenarioRule.scenario.onActivity { activity: LaoActivity ->
      val socialMediaViewModel = obtainSocialMediaViewModel(activity, LAO_ID)
      val viewModel = obtainViewModel(activity)
      val chirpListAdapter =
        createChirpListAdapter(activity, viewModel, socialMediaViewModel, chirps)

      Assert.assertEquals(CHIRP_1, chirpListAdapter.getItem(0))
      Assert.assertEquals(CHIRP_2, chirpListAdapter.getItem(1))
    }
  }

  @Test
  fun itemIdTest() {
    val chirps = createChirpList()
    activityScenarioRule.scenario.onActivity { activity: LaoActivity ->
      val socialMediaViewModel = obtainSocialMediaViewModel(activity, LAO_ID)
      val viewModel = obtainViewModel(activity)
      val chirpListAdapter =
        createChirpListAdapter(activity, viewModel, socialMediaViewModel, chirps)

      Assert.assertEquals(0, chirpListAdapter.getItemId(0))
      Assert.assertEquals(1, chirpListAdapter.getItemId(1))
    }
  }

  @Test
  fun viewMainElementsTest() {
    val chirps = createChirpList()
    activityScenarioRule.scenario.onActivity { activity: LaoActivity ->
      val socialMediaViewModel = obtainSocialMediaViewModel(activity, LAO_ID)
      val viewModel = obtainViewModel(activity)
      val chirpListAdapter =
        createChirpListAdapter(activity, viewModel, socialMediaViewModel, chirps)

      // Use a non null ViewGroup to inflate the card
      val layout = LinearLayout(activity.applicationContext)
      val parent = TextView(activity.applicationContext)
      parent.text = "Mock Title"
      layout.addView(parent)

      // Get the view for the first chirp in the list.
      val view1 = chirpListAdapter.getView(0, null, layout)
      Assert.assertNotNull(view1)

      // Check the text is matching correctly
      val textView = view1.findViewById<TextView>(R.id.social_media_text)
      Assert.assertNotNull(textView)
      Assert.assertEquals(TEXT_1, textView.text.toString())

      // Check the user is matching correctly
      val user = view1.findViewById<TextView>(R.id.social_media_username)
      Assert.assertNotNull(user)
      Assert.assertEquals(SENDER_1.getLabel(), user.text.toString())

      // Check the time is matching correctly
      val time = view1.findViewById<TextView>(R.id.social_media_time)
      Assert.assertNotNull(time)
      Assert.assertEquals(
        DateUtils.getRelativeTimeSpanString(TIMESTAMP_1 * 1000),
        time.text.toString(),
      )

      // Ensure that the buttons are visible
      val buttons = view1.findViewById<LinearLayout>(R.id.chirp_card_buttons)
      Assert.assertNotNull(buttons)
      Assert.assertEquals(View.VISIBLE.toLong(), buttons.visibility.toLong())

      // Assert that the bin is visible
      val bin = view1.findViewById<ImageButton>(R.id.delete_chirp_button)
      Assert.assertNotNull(bin)
      Assert.assertEquals(View.VISIBLE.toLong(), bin.visibility.toLong())

      // Get the view for the second chirp in the list.
      val view2 = chirpListAdapter.getView(1, null, layout)
      Assert.assertNotNull(view2)

      // Assert text is deleted
      val textView2 = view2.findViewById<TextView>(R.id.social_media_text)
      Assert.assertNotNull(textView2)
      Assert.assertEquals(
        activity.applicationContext.getString(R.string.deleted_chirp_2),
        textView2.text.toString(),
      )

      // Assert that the bin is not visible
      val bin2 = view2.findViewById<ImageButton>(R.id.delete_chirp_button)
      Assert.assertNotNull(bin2)
      Assert.assertEquals(View.GONE.toLong(), bin2.visibility.toLong())
    }
  }

  @Test
  fun viewButtonTest() {
    val chirps = createChirpList()
    activityScenarioRule.scenario.onActivity { activity: LaoActivity ->
      val socialMediaViewModel = obtainSocialMediaViewModel(activity, LAO_ID)
      val viewModel = obtainViewModel(activity)
      val chirpListAdapter =
        createChirpListAdapter(activity, viewModel, socialMediaViewModel, chirps)

      // Use a non null ViewGroup to inflate the card
      val layout = LinearLayout(activity.applicationContext)
      val parent = TextView(activity.applicationContext)
      parent.text = "Mock Title"
      layout.addView(parent)

      // Get the view for the first chirp in the list.
      val view1 = chirpListAdapter.getView(0, null, layout)
      Assert.assertNotNull(view1)

      // Verify the upvote is deselected
      val upvoteButton = view1.findViewById<ImageButton>(R.id.upvote_button)
      Assert.assertNotNull(upvoteButton)
      upvoteButton.callOnClick()
      // Remove the upvote reaction
      socialMediaRepository.deleteReaction(LAO_ID, REACTION_ID)
      // Wait for the observable to be notified
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      Assert.assertFalse(upvoteButton.isSelected)

      // Verify the downvote is not set
      val downvoteButton = view1.findViewById<ImageButton>(R.id.downvote_button)
      Assert.assertNotNull(downvoteButton)
      Assert.assertFalse(downvoteButton.isSelected)
      downvoteButton.callOnClick()

      // Verify the heart is not set
      val heartButton = view1.findViewById<ImageButton>(R.id.heart_button)
      Assert.assertNotNull(heartButton)
      Assert.assertFalse(heartButton.isSelected)
      heartButton.callOnClick()
    }
  }

  @Test
  fun profileDisplayTest() {
    val mockSocialMediaViewModel = Mockito.mock(SocialMediaViewModel::class.java)
    Mockito.`when`(mockSocialMediaViewModel.chirps)
            .thenReturn(Observable.just(ArrayList(listOf(CHIRP_1, CHIRP_3))))
    Mockito.`when`(mockSocialMediaViewModel.getReactions(LAO_ID, MESSAGE_ID_1))
            .thenReturn(Observable.just(setOf(REACTION)))
    Mockito.`when`(mockSocialMediaViewModel.getReactions(LAO_ID_2, MESSAGE_ID_3))
            .thenReturn(Observable.just(setOf(REACTION2)))

    val chirps = ArrayList(listOf(CHIRP_1, CHIRP_3))
    activityScenarioRule.scenario.onActivity { activity: LaoActivity ->
      val socialMediaViewModel = mockSocialMediaViewModel
      val viewModel = obtainViewModel(activity)
      val chirpListAdapter =
              createChirpListAdapter(activity, viewModel, socialMediaViewModel, chirps)
      val context = ApplicationProvider.getApplicationContext<Context>()

      // Use a non null ViewGroup to inflate the card
      val layout = LinearLayout(activity.applicationContext)
      val parent = TextView(activity.applicationContext)
      parent.text = "Mock Title"
      layout.addView(parent)

      // Get the view for the first chirp in the list.
      val view1 = chirpListAdapter.getView(0, null, layout)
      Assert.assertNotNull(view1)

      // The chirp of our LAO should be blue
      val profile1 = view1.findViewById<ImageView>(R.id.social_media_profile)
      Assert.assertNotNull(profile1)
      Assert.assertEquals(context.getColor(R.color.colorAccent), profile1.imageTintList?.defaultColor)

      // Get the view for the second chirp in the list.
      val view2 = chirpListAdapter.getView(1, null, layout)
      Assert.assertNotNull(view2)

      // The chirp of the other LAO should be gray
      val profile2 = view2.findViewById<ImageView>(R.id.social_media_profile)
      Assert.assertNotNull(profile2)
      Assert.assertEquals(context.getColor(R.color.gray), profile2.imageTintList?.defaultColor)
    }
  }

  companion object {
    private const val CREATION_TIME: Long = 1631280815
    private const val LAO_NAME_1 = "laoName1"
    private const val LAO_NAME_2 = "laoName2"
    private var SENDER_KEY_1: KeyPair = Base64DataUtils.generatePoPToken()
    private val SENDER_KEY_2: KeyPair = Base64DataUtils.generatePoPToken()
    private val SENDER_1 = SENDER_KEY_1.publicKey
    private val SENDER_2 = SENDER_KEY_2.publicKey
    private val LAO_ID = generateLaoId(SENDER_1, CREATION_TIME, LAO_NAME_1)
    private val LAO_ID_2 = generateLaoId(SENDER_2, CREATION_TIME, LAO_NAME_2)
    private val MESSAGE_ID_1 = Base64DataUtils.generateMessageID()
    private val MESSAGE_ID_2 = Base64DataUtils.generateMessageID()
    private val MESSAGE_ID_3 = Base64DataUtils.generateMessageID()
    private const val TEXT_1 = "text1"
    private const val TEXT_2 = "text2"
    private const val TEXT_3 = "text3"
    private const val TIMESTAMP_1: Long = 1632204910
    private const val TIMESTAMP_2: Long = 1632204900
    private const val TIMESTAMP_3: Long = 1632204905
    private val CHIRP_1 =
            Chirp(MESSAGE_ID_1, SENDER_1, TEXT_1, TIMESTAMP_1, MessageID(""), LAO_ID)
    private val CHIRP_2 =
            Chirp(MESSAGE_ID_2, SENDER_2, TEXT_2, TIMESTAMP_2, MessageID(""), LAO_ID).deleted()
    private val CHIRP_3 =
            Chirp(MESSAGE_ID_3, SENDER_2, TEXT_3, TIMESTAMP_3, MessageID(""), LAO_ID_2)
    private val TIMESTAMP = Instant.now().epochSecond

    private fun createChirpList(): List<Chirp> {
      return ArrayList(listOf(CHIRP_1, CHIRP_2))
    }

    private fun createChirpListAdapter(
      activity: FragmentActivity,
      viewModel: LaoViewModel,
      socialMediaViewModel: SocialMediaViewModel,
      chirps: List<Chirp>?,
    ): ChirpListAdapter {
      val adapter = ChirpListAdapter(activity, socialMediaViewModel, viewModel)
      adapter.replaceList(chirps)
      return adapter
    }
  }
}
