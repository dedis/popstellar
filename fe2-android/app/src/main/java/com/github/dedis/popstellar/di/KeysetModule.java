package com.github.dedis.popstellar.di;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AesGcmKeyManager;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager;
import com.google.crypto.tink.signature.PublicKeySignWrapper;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import javax.inject.Qualifier;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import timber.log.Timber;

@Module
@InstallIn(SingletonComponent.class)
public class KeysetModule {

  private static final String TAG = KeysetModule.class.getSimpleName();

  private static final String DEVICE_KEYSET_NAME = "POP_KEYSET";
  private static final String DEVICE_SHARED_PREF_FILE_NAME = "POP_KEYSET_SP";
  private static final String DEVICE_MASTER_KEY_URI = "android-keystore://POP_MASTER_KEY";

  private static final String WALLET_KEYSET_NAME = "POP_KEYSET_2";
  private static final String WALLET_SHARED_PREF_FILE_NAME = "POP_KEYSET_SP_2";
  private static final String WALLET_MASTER_KEY_URI = "android-keystore://POP_MASTER_KEY_2";

  private KeysetModule() {}

  @Provides
  @DeviceKeyset
  @Singleton
  public static AndroidKeysetManager provideDeviceKeysetManager(
      @ApplicationContext Context applicationContext) {
    try {
      SharedPreferences.Editor editor =
          applicationContext
              .getSharedPreferences(DEVICE_SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)
              .edit();
      editor.apply();

      Ed25519PrivateKeyManager.registerPair(true);
      PublicKeySignWrapper.register();

      CompletableFuture<AndroidKeysetManager> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return new AndroidKeysetManager.Builder()
                      .withSharedPref(
                          applicationContext, DEVICE_KEYSET_NAME, DEVICE_SHARED_PREF_FILE_NAME)
                      .withKeyTemplate(KeyTemplates.get("ED25519_RAW"))
                      .withMasterKeyUri(DEVICE_MASTER_KEY_URI)
                      .build();
                } catch (GeneralSecurityException | IOException e) {
                  throw new SecurityException(e);
                }
              },
              Executors.newSingleThreadExecutor());

      return future.join();
    } catch (GeneralSecurityException e) {
      Timber.tag(TAG).e(e, "Could not retrieve the device keyset from the app");
      throw new SecurityException("Could not retrieve the device keyset from the app", e);
    }
  }

  @Provides
  @WalletKeyset
  @Singleton
  public static AndroidKeysetManager provideWalletKeysetManager(
      @ApplicationContext Context applicationContext) {
    try {
      AesGcmKeyManager.register(true);
      AeadConfig.register();

      CompletableFuture<AndroidKeysetManager> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return new AndroidKeysetManager.Builder()
                      .withSharedPref(
                          applicationContext, WALLET_KEYSET_NAME, WALLET_SHARED_PREF_FILE_NAME)
                      .withKeyTemplate(KeyTemplates.get("AES256_GCM_RAW"))
                      .withMasterKeyUri(WALLET_MASTER_KEY_URI)
                      .build();
                } catch (GeneralSecurityException | IOException e) {
                  throw new SecurityException(e);
                }
              },
              Executors.newSingleThreadExecutor());

      return future.join();
    } catch (GeneralSecurityException e) {
      Timber.tag(TAG).e(e, "Could not retrieve the wallet keyset from the app");
      throw new SecurityException("Could not retrieve the wallet keyset from the app", e);
    }
  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface DeviceKeyset {}

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface WalletKeyset {}
}
