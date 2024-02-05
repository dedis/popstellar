package com.github.dedis.popstellar.di

import com.github.dedis.popstellar.di.KeysetModule.DeviceKeyset
import com.github.dedis.popstellar.di.KeysetModule.WalletKeyset
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager
import com.google.crypto.tink.signature.PublicKeySignWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.security.GeneralSecurityException
import javax.inject.Singleton
import org.mockito.Mockito

/**
 * Test module replacing KeysetModule with mock ones so that it can be independent from an android
 * emulator.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [KeysetModule::class])
object TestKeysetModule {
  @JvmStatic
  @Singleton
  @DeviceKeyset
  @Provides
  fun provideDeviceKeysetManager(): AndroidKeysetManager {
    val manager = Mockito.mock(AndroidKeysetManager::class.java)
    return try {
      Ed25519PrivateKeyManager.registerPair(true)
      PublicKeySignWrapper.register()
      Mockito.`when`(manager.keysetHandle)
        .thenReturn(KeysetHandle.generateNew(KeyTemplates.get("ED25519_RAW")))
      manager
    } catch (e: GeneralSecurityException) {
      error("Could not register security primitives for the mock device keyset", e)
    }
  }

  @JvmStatic
  @Singleton
  @WalletKeyset
  @Provides
  fun provideWalletKeysetManager(): AndroidKeysetManager {
    val manager = Mockito.mock(AndroidKeysetManager::class.java)
    return try {
      AesGcmKeyManager.register(true)
      AeadConfig.register()
      Mockito.`when`(manager.keysetHandle)
        .thenReturn(KeysetHandle.generateNew(KeyTemplates.get("AES128_GCM_RAW")))
      manager
    } catch (e: GeneralSecurityException) {
      error("Could not register security primitives for the mock wallet keyset", e)
    }
  }
}
