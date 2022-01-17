package com.github.dedis.popstellar.utility.security;

import android.util.Log;

import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PrivateKey;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.privatekey.ProtectedPrivateKey;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KeyManager {

  private static final String TAG = KeyManager.class.getSimpleName();

  private final AndroidKeysetManager keysetManager;
  private final Wallet wallet;

  @Inject
  public KeyManager(AndroidKeysetManager keysetManager, Wallet wallet) {
    this.keysetManager = keysetManager;
    this.wallet = wallet;

    try {
      Log.d(TAG, "Public Key = " + getMainPublicKey().getEncoded());
    } catch (IOException | GeneralSecurityException e) {
      Log.e(TAG, "Failed to retrieve public key", e);
      throw new IllegalStateException("Failed to retrieve public key", e);
    }
  }

  public PublicKey getMainPublicKey() throws IOException, GeneralSecurityException {
    return getPublicKey(keysetManager.getKeysetHandle());
  }

  public KeyPair getMainKeyPair() throws IOException, GeneralSecurityException {
    PrivateKey privateKey = new ProtectedPrivateKey(keysetManager.getKeysetHandle());
    PublicKey publicKey = getMainPublicKey();

    return new KeyPair(privateKey, publicKey);
  }

  public PoPToken getPoPToken(String laoID, String rollCallID) throws KeyException {
    return wallet.findKeyPair(laoID, rollCallID);
  }

  public KeyPair getKeyPair(KeysetHandle keysetHandle)
      throws GeneralSecurityException, IOException {
    PrivateKey privateKey = new ProtectedPrivateKey(keysetHandle);
    PublicKey publicKey = getPublicKey(keysetHandle);

    return new KeyPair(privateKey, publicKey);
  }

  private PublicKey getPublicKey(KeysetHandle keysetHandle)
      throws GeneralSecurityException, IOException {
    ByteArrayOutputStream publicKeysetStream = new ByteArrayOutputStream();
    CleartextKeysetHandle.write(
        keysetHandle.getPublicKeysetHandle(),
        JsonKeysetWriter.withOutputStream(publicKeysetStream));

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
