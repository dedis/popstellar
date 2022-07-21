package com.github.dedis.popstellar.di;

import com.github.dedis.popstellar.di.KeysetModule.DeviceKeyset;
import com.github.dedis.popstellar.di.KeysetModule.WalletKeyset;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AesGcmKeyManager;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager;
import com.google.crypto.tink.signature.PublicKeySignWrapper;

import java.security.GeneralSecurityException;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test module replacing KeysetModule with mock ones so that it can be independent from an android
 * emulator.
 */
@Module
@TestInstallIn(components = SingletonComponent.class, replaces = KeysetModule.class)
public class TestKeysetModule {

  @Singleton
  @DeviceKeyset
  @Provides
  public static AndroidKeysetManager provideDeviceKeysetManager() {
    AndroidKeysetManager manager = mock(AndroidKeysetManager.class);

    try {
      Ed25519PrivateKeyManager.registerPair(true);
      PublicKeySignWrapper.register();
      when(manager.getKeysetHandle())
          .thenReturn(KeysetHandle.generateNew(Ed25519PrivateKeyManager.rawEd25519Template()));
      return manager;
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(
          "Could not register security primitives for the mock device keyset", e);
    }
  }

  @Singleton
  @WalletKeyset
  @Provides
  public static AndroidKeysetManager provideWalletKeysetManager() {
    AndroidKeysetManager manager = mock(AndroidKeysetManager.class);

    try {
      AesGcmKeyManager.register(true);
      AeadConfig.register();
      when(manager.getKeysetHandle())
          .thenReturn(KeysetHandle.generateNew(AesGcmKeyManager.rawAes128GcmTemplate()));
      return manager;
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(
          "Could not register security primitives for the mock wallet keyset", e);
    }
  }
}
