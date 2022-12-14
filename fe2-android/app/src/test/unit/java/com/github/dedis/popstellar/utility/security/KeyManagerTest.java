package com.github.dedis.popstellar.utility.security;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.error.keys.*;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;

import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.security.GeneralSecurityException;
import java.util.HashSet;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static com.github.dedis.popstellar.di.KeysetModule.DeviceKeyset;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class KeyManagerTest {

  private final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule public RuleChain rule = RuleChain.outerRule(hiltRule).around(MockitoJUnit.testRule(this));

  @Rule public InstantTaskExecutorRule executorRule = new InstantTaskExecutorRule();

  @Inject @DeviceKeyset AndroidKeysetManager androidKeysetManager;
  @Inject RollCallRepository rollCallRepo;
  @Mock private Wallet wallet;

  @Before
  public void setup() {
    hiltRule.inject();
  }

  @Test
  public void keyPairIsRight() throws GeneralSecurityException {
    KeyManager keyManager = new KeyManager(androidKeysetManager, wallet);
    KeyPair mainKeyPair = keyManager.getMainKeyPair();
    PublicKey mainKey = keyManager.getMainPublicKey();

    assertEquals(mainKey, mainKeyPair.getPublicKey());

    // We cannot extract the public key from the keyset handle
    // But we can make sure both signatures are equals and the key manager keypair can verify it
    PublicKeySign signer = androidKeysetManager.getKeysetHandle().getPrimitive(PublicKeySign.class);
    // Generate any data, here a message id
    Base64URLData data = Base64DataUtils.generateMessageID();

    Signature signature = mainKeyPair.sign(data);
    assertArrayEquals(signer.sign(data.getData()), signature.getData());
    assertTrue(mainKey.verify(signature, data));
  }

  @Test
  public void popTokenRetrievingWorks() throws KeyException {
    PoPToken token = Base64DataUtils.generatePoPToken();
    when(wallet.recoverKey(any(), any(), any())).thenReturn(token);

    // create LAO and RollCalls
    Lao lao = new Lao("lao", Base64DataUtils.generatePublicKey(), 54213424);
    String rollCallName1 = "rollcall1";
    String rollCallName2 = "rollcall2";
    long creation1 = 5421364;
    long creation2 = 5421363;
    String id1 = RollCall.generateCreateRollCallId(lao.getId(), creation1, rollCallName1);
    String id2 = RollCall.generateCreateRollCallId(lao.getId(), creation2, rollCallName2);
    RollCall rollCall1 =
        new RollCall(
            id1,
            id1,
            rollCallName1,
            creation1,
            creation1 + 1,
            creation1 + 75,
            EventState.CLOSED,
            new HashSet<>(),
            "location",
            "desc");
    RollCall rollCall2 =
        new RollCall(
            id2,
            id2,
            rollCallName2,
            creation2,
            creation2 + 1,
            creation2 + 75,
            EventState.CLOSED,
            new HashSet<>(),
            "EPFL",
            "do not come");

    rollCallRepo.updateRollCall(lao.getId(), rollCall1, rollCall1.getId());
    rollCallRepo.updateRollCall(lao.getId(), rollCall2, rollCall2.getId());

    KeyManager manager = new KeyManager(androidKeysetManager, wallet);
    assertEquals(
        token,
        manager.getValidPoPToken(lao.getId(), rollCallRepo.getLastClosedRollCall(lao.getId())));
    assertEquals(token, manager.getValidPoPToken(lao.getId(), rollCall1));

    // make sure that rollcall1 was taken and not rollcall2 as the oldest is rollcall 1
    verify(wallet, atLeast(1)).recoverKey(eq(lao.getId()), eq(rollCall1.getId()), any());
  }

  @Test
  public void popTokenRetrievingFailsWhenWalletFails() throws KeyException {
    PoPToken token = Base64DataUtils.generatePoPToken();

    // create LAO and RollCall
    Lao lao = new Lao("lao", Base64DataUtils.generatePublicKey(), 54213424);
    String id = RollCall.generateCreateRollCallId(lao.getId(), 5421364, "rollcall");
    RollCall rollCall =
        new RollCall(
            id,
            id,
            "rollcall",
            5421364,
            5421364 + 1,
            5421364 + 145,
            EventState.CLOSED,
            new HashSet<>(),
            "ETHZ",
            "do come");

    rollCallRepo.updateRollCall(lao.getId(), rollCall, rollCall.getId());
    KeyManager manager = new KeyManager(androidKeysetManager, wallet);

    // Test with every possible errors
    when(wallet.recoverKey(any(), any(), any()))
        .thenThrow(new KeyGenerationException(new GeneralSecurityException()));
    assertThrows(
        KeyGenerationException.class,
        () ->
            manager.getValidPoPToken(lao.getId(), rollCallRepo.getLastClosedRollCall(lao.getId())));
    verify(wallet, times(1)).recoverKey(eq(lao.getId()), eq(rollCall.getId()), any());
    reset(wallet);

    when(wallet.recoverKey(any(), any(), any())).thenThrow(new UninitializedWalletException());
    assertThrows(
        UninitializedWalletException.class,
        () ->
            manager.getValidPoPToken(lao.getId(), rollCallRepo.getLastClosedRollCall(lao.getId())));
    verify(wallet, times(1)).recoverKey(eq(lao.getId()), eq(rollCall.getId()), any());
    reset(wallet);

    when(wallet.recoverKey(any(), any(), any())).thenThrow(new InvalidPoPTokenException(token));
    assertThrows(
        InvalidPoPTokenException.class,
        () ->
            manager.getValidPoPToken(lao.getId(), rollCallRepo.getLastClosedRollCall(lao.getId())));
    verify(wallet, times(1)).recoverKey(eq(lao.getId()), eq(rollCall.getId()), any());
  }

  @Test
  public void popTokenRetrievingFailsWhenLaoHasNoRollCall() {
    // create LAO
    Lao lao = new Lao("lao", Base64DataUtils.generatePublicKey(), 54213424);

    KeyManager manager = new KeyManager(androidKeysetManager, wallet);
    assertThrows(
        NoRollCallException.class,
        () ->
            manager.getValidPoPToken(lao.getId(), rollCallRepo.getLastClosedRollCall(lao.getId())));
  }
}
