package com.github.dedis.popstellar.utility.security

import androidx.annotation.VisibleForTesting
import com.github.dedis.popstellar.di.KeysetModule.DeviceKeyset
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.model.objects.security.AuthToken
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.model.objects.security.PrivateKey
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.privatekey.ProtectedPrivateKey
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.Arrays
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Service managing keys and providing easy access to the main device key and PoP Tokens */
@Singleton
class KeyManager
@Inject
constructor(
    @param:DeviceKeyset private val keysetManager: AndroidKeysetManager,
    private val wallet: Wallet
) {

  /** the device keypair */
  lateinit var mainKeyPair: KeyPair
    private set

  init {
    fun throwException(e: Exception) {
      Timber.tag(TAG).e(e, "Failed to retrieve device's key")
      error("Failed to retrieve device's key", e)
    }

    try {
      cacheMainKey()
      Timber.tag(TAG).d("Public Key = %s", mainPublicKey.encoded)
    } catch (e: Exception) {
      when (e) {
        is IOException,
        is GeneralSecurityException -> throwException(e)
        else -> {}
      }
    }
  }

  /**
   * This will cache the device KeyPair by extracting it from Tink.
   *
   * Use this only if you know what you are doing
   *
   * @throws IOException when the key cannot be retrieved due to IO errors
   * @throws GeneralSecurityException when the retrieved key is not valid
   */
  @Throws(GeneralSecurityException::class, IOException::class)
  private fun cacheMainKey() {
    mainKeyPair = getKeyPair(keysetManager.keysetHandle)
  }

  val mainPublicKey: PublicKey
    /** @return the device public key */
    get() = mainKeyPair.publicKey

  /**
   * Generate the PoP Token for the given Lao - RollCall pair
   *
   * @param laoView to generate the PoP Token from
   * @param rollCall to generate the PoP Token from
   * @return the generated PoP Token
   * @throws KeyGenerationException if an error occurs during key generation
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   */
  @Throws(KeyException::class)
  fun getPoPToken(laoView: LaoView, rollCall: RollCall): PoPToken {
    return wallet.generatePoPToken(laoView.id, rollCall.persistentId)
  }

  /**
   * Generate a long-term Authentication Token for the given Lao - ClientID pair
   *
   * @param laoId to generate the AuthToken from
   * @param clientId to generate the AuthToken from
   * @return the generated AuthToken
   * @throws KeyGenerationException if an error occurs during key generation
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   */
  @Throws(KeyException::class)
  fun getLongTermAuthToken(laoId: String, clientId: String): AuthToken {
    return AuthToken(wallet.generatePoPToken(laoId, HashSHA256.hash(clientId)))
  }

  /**
   * Try to retrieve the user's PoPToken for the given Lao and RollCall. It will fail if the user
   * did not attend the roll call or if the token cannot be generated
   *
   * @param laoId of the lao we want to retrieve the PoP Token from
   * @param rollCall we want to retrieve the PoP Token from
   * @return the generated token if present in the rollcall
   * @throws KeyGenerationException if an error occurs during key generation
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   * @throws InvalidPoPTokenException if the token is not a valid attendee
   */
  @Throws(KeyException::class)
  fun getValidPoPToken(laoId: String, rollCall: RollCall): PoPToken {
    return wallet.recoverKey(laoId, rollCall.persistentId, rollCall.attendees)
  }

  @VisibleForTesting
  @Throws(GeneralSecurityException::class, IOException::class)
  fun getKeyPair(keysetHandle: KeysetHandle): KeyPair {
    val privateKey: PrivateKey = ProtectedPrivateKey(keysetHandle)
    val publicKey = getPublicKey(keysetHandle)

    return KeyPair(privateKey, publicKey)
  }

  @Throws(GeneralSecurityException::class, IOException::class)
  private fun getPublicKey(keysetHandle: KeysetHandle): PublicKey {
    // Retrieve the public key from the keyset. This is not an easy task and thanks to this post :
    // https://stackoverflow.com/questions/53228475/google-tink-how-use-public-key-to-verify-signature
    // A solution was found
    val publicKeysetStream = ByteArrayOutputStream()
    CleartextKeysetHandle.write(
        keysetHandle.publicKeysetHandle, JsonKeysetWriter.withOutputStream(publicKeysetStream))

    // The "publickey" is still a json data. We need to extract the actual key from it
    val publicKeyJson = JsonParser.parseString(publicKeysetStream.toString())
    val root = publicKeyJson.asJsonObject
    val keyArray = root["key"].asJsonArray
    val keyObject = keyArray[0].asJsonObject
    val keyData = keyObject["keyData"].asJsonObject
    val buffer = Base64.getDecoder().decode(keyData["value"].asString)

    // Remove the first two bytes of the buffer as they are not part of the key
    val publicKey = Arrays.copyOfRange(buffer, 2, buffer.size)
    return PublicKey(publicKey)
  }

  companion object {
    private val TAG = KeyManager::class.java.simpleName
  }
}
