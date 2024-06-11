package com.github.dedis.popstellar.ui.lao

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Channel.Companion.getLaoChannel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsEntity
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.IntentUtils
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.utility.GeneralUtils.generateUsernameFromBase64
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import javax.inject.Inject
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LaoActivityTest {
  @Inject lateinit var appDatabase: AppDatabase

  @Inject lateinit var globalNetworkManager: GlobalNetworkManager

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      override fun before() {
        hiltRule.inject()
        val subscriptionsEntity = SubscriptionsEntity(LAO.id, URL, CHANNELS)
        appDatabase.subscriptionsDao().insert(subscriptionsEntity).test().awaitTerminalEvent()
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule =
    ActivityScenarioRule<LaoActivity>(
      IntentUtils.createIntent(
        LaoActivity::class.java,
        BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO.id).build()
      )
    )

  @Test
  fun restoreConnectionsTest() {
    Assert.assertEquals(CHANNELS, globalNetworkManager.messageSender.subscriptions)
  }

  @Test
  fun testMnemonic() {
    //generate a list of different public keys generated
    val base64List = listOf(
      Base64DataUtils.generatePublicKey().encoded,
      Base64DataUtils.generatePublicKey().encoded,
      Base64DataUtils.generatePublicKey().encoded,
      Base64DataUtils.generatePublicKey().encoded,
      Base64DataUtils.generatePublicKey().encoded,
      Base64DataUtils.generatePublicKey().encoded
    )
    val mnemonics = base64List.map {b64 -> generateUsernameFromBase64(b64) }
    println("_______________________________________________________")
    println("Base64: ${base64List.joinToString()}")
    println("Mnemonics: ${mnemonics.joinToString()}")
    println("_______________________________________________________")
  }

  companion object {
    private const val LAO_NAME = "LAO"
    private val KEY_PAIR = Base64DataUtils.generateKeyPair()
    private val PK = KEY_PAIR.publicKey
    private val LAO = Lao(LAO_NAME, PK, Instant.now().epochSecond)
    private const val URL = "url"
    private val CHANNELS: MutableSet<Channel> =
      mutableSetOf(getLaoChannel(LAO.id), getLaoChannel(LAO.id).subChannel("random"))
  }
}
