package com.github.dedis.popstellar.utility.security;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.error.keys.InvalidPoPTokenException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.KeyGenerationException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.error.keys.UninitializedWalletException;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.security.GeneralSecurityException;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import static com.github.dedis.popstellar.di.KeysetModule.DeviceKeyset;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class KeyManagerTest {

  private final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule public RuleChain rule = RuleChain.outerRule(hiltRule).around(MockitoJUnit.testRule(this));

  @Rule public InstantTaskExecutorRule executorRule = new InstantTaskExecutorRule();

  @Inject @DeviceKeyset AndroidKeysetManager androidKeysetManager;
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
    RollCall rollCall1 = new RollCall(lao.getId(), 5421364, "rollcall1");
    RollCall rollCall2 = new RollCall(lao.getId(), 5421363, "rollcall2");

    rollCall1.setState(EventState.CLOSED);
    rollCall2.setState(EventState.CLOSED);

    lao.updateRollCall(rollCall1.getId(), rollCall1);
    lao.updateRollCall(rollCall2.getId(), rollCall2);

    KeyManager manager = new KeyManager(androidKeysetManager, wallet);
    assertEquals(token, manager.getValidPoPToken(lao));
    assertEquals(token, manager.getValidPoPToken(lao, rollCall1));

    // make sure that rollcall1 was taken and not rollcall2 as the oldest is rollcall 1
    verify(wallet, atLeast(1)).recoverKey(eq(lao.getId()), eq(rollCall1.getId()), any());
  }

  @Test
  public void popTokenRetrievingFailsWhenWalletFails() throws KeyException {
    PoPToken token = Base64DataUtils.generatePoPToken();

    // create LAO and RollCall
    Lao lao = new Lao("lao", Base64DataUtils.generatePublicKey(), 54213424);
    RollCall rollCall = new RollCall(lao.getId(), 5421364, "rollcall");
    rollCall.setState(EventState.CLOSED);
    lao.updateRollCall(rollCall.getId(), rollCall);

    KeyManager manager = new KeyManager(androidKeysetManager, wallet);

    // Test with every possible errors
    when(wallet.recoverKey(any(), any(), any()))
        .thenThrow(new KeyGenerationException(new GeneralSecurityException()));
    assertThrows(KeyGenerationException.class, () -> manager.getValidPoPToken(lao));
    verify(wallet, times(1)).recoverKey(eq(lao.getId()), eq(rollCall.getId()), any());
    reset(wallet);

    when(wallet.recoverKey(any(), any(), any())).thenThrow(new UninitializedWalletException());
    assertThrows(UninitializedWalletException.class, () -> manager.getValidPoPToken(lao));
    verify(wallet, times(1)).recoverKey(eq(lao.getId()), eq(rollCall.getId()), any());
    reset(wallet);

    when(wallet.recoverKey(any(), any(), any())).thenThrow(new InvalidPoPTokenException(token));
    assertThrows(InvalidPoPTokenException.class, () -> manager.getValidPoPToken(lao));
    verify(wallet, times(1)).recoverKey(eq(lao.getId()), eq(rollCall.getId()), any());
  }

  @Test
  public void popTokenRetrievingFailsWhenLaoHasNoRollCall() {
    // create LAO
    Lao lao = new Lao("lao", Base64DataUtils.generatePublicKey(), 54213424);

    KeyManager manager = new KeyManager(androidKeysetManager, wallet);
    assertThrows(NoRollCallException.class, () -> manager.getValidPoPToken(lao));
  }
}
