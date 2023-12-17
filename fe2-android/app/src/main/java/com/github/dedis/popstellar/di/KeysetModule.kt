package com.github.dedis.popstellar.di

import android.content.Context
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager
import com.google.crypto.tink.signature.PublicKeySignWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KeysetModule {
    private val TAG = KeysetModule::class.java.simpleName
    private const val DEVICE_KEYSET_NAME = "POP_KEYSET"
    private const val DEVICE_SHARED_PREF_FILE_NAME = "POP_KEYSET_SP"
    private const val DEVICE_MASTER_KEY_URI = "android-keystore://POP_MASTER_KEY"
    private const val WALLET_KEYSET_NAME = "POP_KEYSET_2"
    private const val WALLET_SHARED_PREF_FILE_NAME = "POP_KEYSET_SP_2"
    private const val WALLET_MASTER_KEY_URI = "android-keystore://POP_MASTER_KEY_2"
    @Provides
    @DeviceKeyset
    @Singleton
    fun provideDeviceKeysetManager(
        @ApplicationContext applicationContext: Context
    ): AndroidKeysetManager {
        return try {
            val editor = applicationContext
                .getSharedPreferences(DEVICE_SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)
                .edit()
            editor.apply()
            Ed25519PrivateKeyManager.registerPair(true)
            PublicKeySignWrapper.register()
            val future = CompletableFuture.supplyAsync(
                {
                    try {
                        return@supplyAsync AndroidKeysetManager.Builder()
                            .withSharedPref(
                                applicationContext, DEVICE_KEYSET_NAME, DEVICE_SHARED_PREF_FILE_NAME
                            )
                            .withKeyTemplate(KeyTemplates.get("ED25519_RAW"))
                            .withMasterKeyUri(DEVICE_MASTER_KEY_URI)
                            .build()
                    } catch (e: GeneralSecurityException) {
                        throw SecurityException(e)
                    } catch (e: IOException) {
                        throw SecurityException(e)
                    }
                },
                Executors.newSingleThreadExecutor()
            )
            future.join()
        } catch (e: GeneralSecurityException) {
            Timber.tag(TAG).e(e, "Could not retrieve the device keyset from the app")
            throw SecurityException("Could not retrieve the device keyset from the app", e)
        }
    }

    @Provides
    @WalletKeyset
    @Singleton
    fun provideWalletKeysetManager(
        @ApplicationContext applicationContext: Context?
    ): AndroidKeysetManager {
        return try {
            AesGcmKeyManager.register(true)
            AeadConfig.register()
            val future = CompletableFuture.supplyAsync(
                {
                    try {
                        return@supplyAsync AndroidKeysetManager.Builder()
                            .withSharedPref(
                                applicationContext, WALLET_KEYSET_NAME, WALLET_SHARED_PREF_FILE_NAME
                            )
                            .withKeyTemplate(KeyTemplates.get("AES256_GCM_RAW"))
                            .withMasterKeyUri(WALLET_MASTER_KEY_URI)
                            .build()
                    } catch (e: GeneralSecurityException) {
                        throw SecurityException(e)
                    } catch (e: IOException) {
                        throw SecurityException(e)
                    }
                },
                Executors.newSingleThreadExecutor()
            )
            future.join()
        } catch (e: GeneralSecurityException) {
            Timber.tag(TAG).e(e, "Could not retrieve the wallet keyset from the app")
            throw SecurityException("Could not retrieve the wallet keyset from the app", e)
        }
    }

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class DeviceKeyset

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class WalletKeyset
}