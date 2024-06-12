package com.github.dedis.popstellar.utility.security

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.KeysetModule.DeviceKeyset
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.RollCall.Companion.generateCreateRollCallId
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.utility.error.keys.InvalidPoPTokenException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.KeyGenerationException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.error.keys.UninitializedWalletException
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.security.GeneralSecurityException
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class KeyManagerTest {
  private val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule
  var rule: RuleChain = RuleChain.outerRule(hiltRule).around(MockitoJUnit.testRule(this))

  @JvmField @Rule var executorRule = InstantTaskExecutorRule()

  @Inject @DeviceKeyset lateinit var androidKeysetManager: AndroidKeysetManager

  @Inject lateinit var rollCallRepo: RollCallRepository

  @Mock private lateinit var wallet: Wallet

  @Before
  fun setup() {
    hiltRule.inject()
  }

  @Test
  @Throws(GeneralSecurityException::class)
  fun keyPairIsRight() {
    val keyManager = KeyManager(androidKeysetManager, wallet)
    val mainKeyPair = keyManager.mainKeyPair
    val mainKey = keyManager.mainPublicKey

    Assert.assertEquals(mainKey, mainKeyPair.publicKey)

    // We cannot extract the public key from the keyset handle
    // But we can make sure both signatures are equals and the key manager keypair can verify it
    val signer = androidKeysetManager.keysetHandle.getPrimitive(PublicKeySign::class.java)

    // Generate any data, here a message id
    val data: Base64URLData = Base64DataUtils.generateMessageID()
    val signature = mainKeyPair.sign(data)

    Assert.assertArrayEquals(signer.sign(data.data), signature.data)
    Assert.assertTrue(mainKey.verify(signature, data))
  }

  @Test
  @Throws(KeyException::class)
  fun popTokenRetrievingWorks() {
    val token = Base64DataUtils.generatePoPToken()
    Mockito.`when`(
        wallet.recoverKey(
          MockitoKotlinHelpers.any(),
          MockitoKotlinHelpers.any(),
          MockitoKotlinHelpers.any()
        )
      )
      .thenReturn(token)

    // create LAO and RollCalls
    val lao = Lao("lao", Base64DataUtils.generatePublicKey(), 54213424)
    val rollCallName1 = "rollcall1"
    val rollCallName2 = "rollcall2"
    val creation1: Long = 5421364
    val creation2: Long = 5421363
    val id1 = generateCreateRollCallId(lao.id, creation1, rollCallName1)
    val id2 = generateCreateRollCallId(lao.id, creation2, rollCallName2)
    val rollCall1 =
      RollCall(
        id1,
        id1,
        rollCallName1,
        creation1,
        creation1 + 1,
        creation1 + 75,
        EventState.CLOSED,
        LinkedHashSet(),
        "location",
        "desc"
      )
    val rollCall2 =
      RollCall(
        id2,
        id2,
        rollCallName2,
        creation2,
        creation2 + 1,
        creation2 + 75,
        EventState.CLOSED,
        LinkedHashSet(),
        "EPFL",
        "do not come"
      )
    rollCallRepo.updateRollCall(lao.id, rollCall1)
    rollCallRepo.updateRollCall(lao.id, rollCall2)

    val manager = KeyManager(androidKeysetManager, wallet)

    Assert.assertEquals(
      token,
      manager.getValidPoPToken(lao.id, rollCallRepo.getLastClosedRollCall(lao.id))
    )
    Assert.assertEquals(token, manager.getValidPoPToken(lao.id, rollCall1))

    // make sure that rollcall1 was taken and not rollcall2 as the oldest is rollcall 1
    Mockito.verify(wallet, Mockito.atLeast(1))
      .recoverKey(
        MockitoKotlinHelpers.eq(lao.id),
        MockitoKotlinHelpers.eq(rollCall1.id),
        MockitoKotlinHelpers.any()
      )
  }

  @Test
  @Throws(KeyException::class)
  fun popTokenRetrievingFailsWhenWalletFails() {
    val token = Base64DataUtils.generatePoPToken()

    // create LAO and RollCall
    val lao = Lao("lao", Base64DataUtils.generatePublicKey(), 54213424)
    val id = generateCreateRollCallId(lao.id, 5421364, "rollcall")
    val rollCall =
      RollCall(
        id,
        id,
        "rollcall",
        5421364,
        (5421364 + 1).toLong(),
        (5421364 + 145).toLong(),
        EventState.CLOSED,
        LinkedHashSet(),
        "ETHZ",
        "do come"
      )
    rollCallRepo.updateRollCall(lao.id, rollCall)

    val manager = KeyManager(androidKeysetManager, wallet)
    // Test with every possible errors
    Mockito.`when`(
        wallet.recoverKey(
          MockitoKotlinHelpers.any(),
          MockitoKotlinHelpers.any(),
          MockitoKotlinHelpers.any()
        )
      )
      .thenThrow(KeyGenerationException(GeneralSecurityException()))

    Assert.assertThrows(KeyGenerationException::class.java) {
      manager.getValidPoPToken(lao.id, rollCallRepo.getLastClosedRollCall(lao.id))
    }
    Mockito.verify(wallet, Mockito.times(1))
      .recoverKey(
        MockitoKotlinHelpers.eq(lao.id),
        MockitoKotlinHelpers.eq(rollCall.id),
        MockitoKotlinHelpers.any()
      )

    Mockito.reset(wallet)

    Mockito.`when`(
        wallet.recoverKey(
          MockitoKotlinHelpers.any(),
          MockitoKotlinHelpers.any(),
          MockitoKotlinHelpers.any()
        )
      )
      .thenThrow(UninitializedWalletException())

    Assert.assertThrows(UninitializedWalletException::class.java) {
      manager.getValidPoPToken(lao.id, rollCallRepo.getLastClosedRollCall(lao.id))
    }
    Mockito.verify(wallet, Mockito.times(1))
      .recoverKey(
        MockitoKotlinHelpers.eq(lao.id),
        MockitoKotlinHelpers.eq(rollCall.id),
        MockitoKotlinHelpers.any()
      )

    Mockito.reset(wallet)

    Mockito.`when`(
        wallet.recoverKey(
          MockitoKotlinHelpers.any(),
          MockitoKotlinHelpers.any(),
          MockitoKotlinHelpers.any()
        )
      )
      .thenThrow(InvalidPoPTokenException(token))

    Assert.assertThrows(InvalidPoPTokenException::class.java) {
      manager.getValidPoPToken(lao.id, rollCallRepo.getLastClosedRollCall(lao.id))
    }
    Mockito.verify(wallet, Mockito.times(1))
      .recoverKey(
        MockitoKotlinHelpers.eq(lao.id),
        MockitoKotlinHelpers.eq(rollCall.id),
        MockitoKotlinHelpers.any()
      )
  }

  @Test
  fun popTokenRetrievingFailsWhenLaoHasNoRollCall() {
    // create LAO
    val lao = Lao("lao", Base64DataUtils.generatePublicKey(), 54213424)
    val manager = KeyManager(androidKeysetManager, wallet)
    Assert.assertThrows(NoRollCallException::class.java) {
      manager.getValidPoPToken(lao.id, rollCallRepo.getLastClosedRollCall(lao.id))
    }
  }
}
