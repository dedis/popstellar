package com.github.dedis.popstellar.utility.security;

import android.util.Base64;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Keys {

  public static String getEncodedKey(KeysetHandle handle) throws IOException {
    ByteArrayOutputStream publicKeysetStream = new ByteArrayOutputStream();
    CleartextKeysetHandle.write(handle, JsonKeysetWriter.withOutputStream(publicKeysetStream));

    JsonElement publicKeyJson = JsonParser.parseString(publicKeysetStream.toString());
    JsonObject root = publicKeyJson.getAsJsonObject();
    JsonArray keyArray = root.get("key").getAsJsonArray();
    JsonObject keyObject = keyArray.get(0).getAsJsonObject();
    JsonObject keyData = keyObject.get("keyData").getAsJsonObject();

    String encoded = keyData.get("value").getAsString();
    byte[] buf = Base64.decode(encoded, Base64.NO_WRAP);
    return Base64.encodeToString(buf, 2, 32, Base64.NO_WRAP | Base64.URL_SAFE);
  }
}
