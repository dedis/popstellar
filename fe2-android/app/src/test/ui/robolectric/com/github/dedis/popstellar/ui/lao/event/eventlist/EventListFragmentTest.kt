package com.github.dedis.popstellar.ui.lao.event.eventlist

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.IntentUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.event.EvenListFragmentPageObject
import com.github.dedis.popstellar.testutils.pages.lao.event.election.ElectionSetupPageObject
import com.github.dedis.popstellar.testutils.pages.lao.event.rollcall.RollCallCreatePageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.handler.MessageHandler
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.FormatException
import com.google.zxing.LuminanceSource
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Completable
import javax.inject.Inject
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EventListFragmentTest {
  @Inject lateinit var gson: Gson

  @Inject lateinit var handler: MessageHandler

  @Inject lateinit var laoRepository: LAORepository

  @BindValue @Mock lateinit var networkManager: GlobalNetworkManager

  @BindValue @Mock lateinit var keyManager: KeyManager

  @Mock lateinit var messageSender: MessageSender

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

        laoRepository.updateLao(LAO)
        Mockito.`when`(keyManager.mainKeyPair).thenReturn(KEY_PAIR)
        Mockito.`when`(keyManager.mainPublicKey).thenReturn(PK)
        Mockito.`when`(
            keyManager.getPoPToken(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
          )
          .thenReturn(POP_TOKEN)
        Mockito.`when`(networkManager.messageSender).thenReturn(messageSender)
        Mockito.`when`(
            messageSender.publish(
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any()
            )
          )
          .thenReturn(Completable.complete())
        Mockito.`when`(
            messageSender.publish(
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any(CreateRollCall::class.java)
            )
          )
          .thenAnswer { answer: InvocationOnMock ->
            val sender = answer.getArgument<KeyPair>(0)
            val channel = answer.getArgument<Channel>(1)
            val data = answer.getArgument<Data>(2)

            // Do handle create roll call message
            handler.handleMessage(messageSender, channel, MessageGeneral(sender, data, gson))
            Completable.complete()
          }
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule =
    ActivityScenarioRule<LaoActivity>(
      IntentUtils.createIntent(
        LaoActivity::class.java,
        BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build()
      )
    )

  @Test
  fun addEventButtonIsDisplayedForOrganizer() {
    EvenListFragmentPageObject.addEventButton()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun addRCAndElectionButtonsAndTextsAreNotDisplayed() {
    EvenListFragmentPageObject.addElectionButton()
      .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
    EvenListFragmentPageObject.addRollCallButton()
      .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
    EvenListFragmentPageObject.addElectionText()
      .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
    EvenListFragmentPageObject.addRollCallText()
      .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
  }

  @Test
  fun addRCAndElectionButtonsAndTextsAreDisplayedWhenAddIsClicked() {
    EvenListFragmentPageObject.addEventButton().perform(ViewActions.click())

    EvenListFragmentPageObject.addElectionButton()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    EvenListFragmentPageObject.addRollCallButton()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    EvenListFragmentPageObject.addElectionText()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    EvenListFragmentPageObject.addRollCallText()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun eventListIsDisplayed() {
    EvenListFragmentPageObject.eventList().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun confirmingRollCallOpensEventListScreen() {
    goToRollCallCreationAndEnterTitleAndLocation()
    RollCallCreatePageObject.rollCallCreateConfirmButton().perform(ViewActions.click())
    LaoActivityPageObject.fragmentContainer()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withChild(ViewMatchers.withId(LaoActivityPageObject.laoDetailFragmentId()))
        )
      )
  }

  @Test
  fun submitElectionOpensEventList() {
    createElection()
    LaoActivityPageObject.fragmentContainer()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withChild(ViewMatchers.withId(LaoActivityPageObject.laoDetailFragmentId()))
        )
      )
  }

  private fun goToRollCallCreationAndEnterTitleAndLocation() {
    EvenListFragmentPageObject.addEventButton().perform(ViewActions.click())
    EvenListFragmentPageObject.addRollCallButton().perform(ViewActions.click())
    RollCallCreatePageObject.rollCallCreateTitle().perform(ViewActions.typeText(RC_NAME))
    RollCallCreatePageObject.rollCallCreateLocation().perform(ViewActions.typeText(RC_LOCATION))
  }

  private fun createElection() {
    EvenListFragmentPageObject.addEventButton().perform(ViewActions.click())
    EvenListFragmentPageObject.addElectionButton().perform(ViewActions.click())
    ElectionSetupPageObject.electionName().perform(ViewActions.typeText(ELECTION_NAME))
    ElectionSetupPageObject.questionText().perform(ViewActions.typeText(QUESTION))
    ElectionSetupPageObject.ballotOptionAtPosition(0).perform(ViewActions.typeText(BALLOT_1))
    ElectionSetupPageObject.ballotOptionAtPosition(1).perform(ViewActions.typeText(BALLOT_2))
    ElectionSetupPageObject.submit().perform(ViewActions.click())
  }

  // Matches an ImageView containing a QRCode with expected content
  private fun withQrCode(expectedContent: String): Matcher<in View> {
    return object : BoundedMatcher<View?, ImageView>(ImageView::class.java) {
      override fun matchesSafely(item: ImageView): Boolean {
        val actualContent = extractContent(item)
        return expectedContent == actualContent
      }

      override fun describeTo(description: Description) {
        description.appendText("QRCode('$expectedContent')")
      }

      override fun describeMismatch(item: Any, description: Description) {
        if (super.matches(item)) {
          // The type is a match, so the mismatch came from the QRCode content
          val content = extractContent(item as ImageView)
          description.appendText("QRCode('$content')")
        } else {
          // The mismatch is on the type, let BoundedMatcher handle it
          super.describeMismatch(item, description)
        }
      }

      private fun extractContent(item: ImageView): String {
        val drawable = item.drawable
        require(drawable is BitmapDrawable) { "The provided ImageView does not contain a bitmap" }
        val binary = convertToBinary(drawable.bitmap)
        try {
          // Parse the bitmap and check it against expected value
          return QRCodeReader().decode(binary).text
        } catch (e: Exception) {
          when (e) {
            is NotFoundException,
            is ChecksumException,
            is FormatException ->
              throw IllegalArgumentException("The provided image is not a valid QRCode", e)
            else -> throw e
          }
        }
      }

      private fun convertToBinary(qrcode: Bitmap): BinaryBitmap {
        // Convert the QRCode to something zxing understands
        val buffer = IntArray(qrcode.width * qrcode.height)
        qrcode.getPixels(buffer, 0, qrcode.width, 0, 0, qrcode.width, qrcode.height)
        val source: LuminanceSource = RGBLuminanceSource(qrcode.width, qrcode.height, buffer)
        return BinaryBitmap(HybridBinarizer(source))
      }
    }
  }

  companion object {
    private const val LAO_NAME = "LAO"
    private val KEY_PAIR = Base64DataUtils.generateKeyPair()
    private val POP_TOKEN = Base64DataUtils.generatePoPToken()
    private val PK = KEY_PAIR.publicKey
    private val LAO = Lao(LAO_NAME, PK, 10223421)
    private val LAO_ID = LAO.id
    private const val RC_NAME = "Roll-Call Title"
    private const val RC_LOCATION = "Not Lausanne"
    private const val ELECTION_NAME = "an election name"
    private const val QUESTION = "question"
    private const val BALLOT_1 = "ballot 1"
    private const val BALLOT_2 = "ballot 2"
  }
}
