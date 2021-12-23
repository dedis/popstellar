package com.github.dedis.popstellar.utility.security;

import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PrivateKey;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.privatekey.ProtectedPrivateKey;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KeyManager {

  private final AndroidKeysetManager keysetManager;
  private final Wallet wallet;

  @Inject
  public KeyManager(AndroidKeysetManager keysetManager, Wallet wallet) {
    this.keysetManager = keysetManager;
    this.wallet = wallet;
  }

  public PublicKey getMainPublicKey() throws IOException, GeneralSecurityException {
    ByteArrayOutputStream publicKeysetStream = new ByteArrayOutputStream();
    CleartextKeysetHandle.write(
        keysetManager.getKeysetHandle().getPublicKeysetHandle(),
        JsonKeysetWriter.withOutputStream(publicKeysetStream));

    JsonElement publicKeyJson = JsonParser.parseString(publicKeysetStream.toString());
    JsonObject root = publicKeyJson.getAsJsonObject();
    JsonArray keyArray = root.get("key").getAsJsonArray();
    JsonObject keyObject = keyArray.get(0).getAsJsonObject();
    JsonObject keyData = keyObject.get("keyData").getAsJsonObject();

    return new PublicKey(keyData.get("value").getAsString());
  }

  public KeyPair getMainKey() throws IOException, GeneralSecurityException {
    PrivateKey privateKey = new ProtectedPrivateKey(keysetManager.getKeysetHandle());
    PublicKey publicKey = getMainPublicKey();

    return new KeyPair(privateKey, publicKey);
  }

  public PoPToken getPoPToken(String laoID, String rollCallID) throws GeneralSecurityException {
    return wallet.findKeyPair(laoID, rollCallID);
  }
}
