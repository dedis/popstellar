package com.github.dedis.popstellar.utility.security;

import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.security.privatekey.ProtectedPrivateKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.utility.error.keys.*;
import com.google.crypto.tink.*;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.github.dedis.popstellar.di.KeysetModule.DeviceKeyset;

/** Service managing keys and providing easy access to the main device key and PoP Tokens */
@Singleton
public class KeyManager {

  private static final String TAG = KeyManager.class.getSimpleName();

  private final AndroidKeysetManager keysetManager;
  private final Wallet wallet;
  private KeyPair keyPair;

  @Inject
  public KeyManager(@DeviceKeyset AndroidKeysetManager keysetManager, Wallet wallet) {
    this.keysetManager = keysetManager;
    this.wallet = wallet;

    try {
      cacheMainKey();
      Log.d(TAG, "Public Key = " + getMainPublicKey().getEncoded());
    } catch (IOException | GeneralSecurityException e) {
      Log.e(TAG, "Failed to retrieve device's key", e);
      throw new IllegalStateException("Failed to retrieve device's key", e);
    }
  }

  /**
   * This will cache the device KeyPair by extracting it from Tink.
   *
   * <p>Use this only if you know what you are doing
   *
   * @throws IOException when the key cannot be retrieved due to IO errors
   * @throws GeneralSecurityException when the retrieved key is not valid
   */
  private void cacheMainKey() throws GeneralSecurityException, IOException {
    keyPair = getKeyPair(keysetManager.getKeysetHandle());
  }

  /**
   * @return the device public key
   */
  public PublicKey getMainPublicKey() {
    return keyPair.getPublicKey();
  }

  /**
   * @return the device keypair
   */
  public KeyPair getMainKeyPair() {
    return keyPair;
  }

  /**
   * Generate the PoP Token for the given Lao - RollCall pair
   *
   * @param laoView to generate the PoP Token from
   * @param rollCall to generate the PoP Token from
   * @return the generated PoP Token
   * @throws KeyGenerationException if an error occurs during key generation
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   */
  public PoPToken getPoPToken(LaoView laoView, RollCall rollCall) throws KeyException {
    return wallet.generatePoPToken(laoView.getId(), rollCall.getPersistentId());
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
  public PoPToken getValidPoPToken(String laoId, RollCall rollCall) throws KeyException {
    return wallet.recoverKey(laoId, rollCall.getPersistentId(), rollCall.getAttendees());
  }

  @VisibleForTesting
  public KeyPair getKeyPair(KeysetHandle keysetHandle)
      throws GeneralSecurityException, IOException {
    PrivateKey privateKey = new ProtectedPrivateKey(keysetHandle);
    PublicKey publicKey = getPublicKey(keysetHandle);

    return new KeyPair(privateKey, publicKey);
  }

  private PublicKey getPublicKey(KeysetHandle keysetHandle)
      throws GeneralSecurityException, IOException {
    // Retrieve the public key from the keyset. This is not an easy task and thanks to this post :
    // https://stackoverflow.com/questions/53228475/google-tink-how-use-public-key-to-verify-signature
    // A solution was found
    ByteArrayOutputStream publicKeysetStream = new ByteArrayOutputStream();
    CleartextKeysetHandle.write(
        keysetHandle.getPublicKeysetHandle(),
        JsonKeysetWriter.withOutputStream(publicKeysetStream));

    // The "publickey" is still a json data. We need to extract the actual key from it
    JsonElement publicKeyJson = JsonParser.parseString(publicKeysetStream.toString());
    JsonObject root = publicKeyJson.getAsJsonObject();
    JsonArray keyArray = root.get("key").getAsJsonArray();
    JsonObject keyObject = keyArray.get(0).getAsJsonObject();
    JsonObject keyData = keyObject.get("keyData").getAsJsonObject();

    byte[] buffer = Base64.getDecoder().decode(keyData.get("value").getAsString());

    // Remove the first two bytes of the buffer as they are not part of the key
    byte[] publicKey = Arrays.copyOfRange(buffer, 2, buffer.length);
    return new PublicKey(publicKey);
  }
}
