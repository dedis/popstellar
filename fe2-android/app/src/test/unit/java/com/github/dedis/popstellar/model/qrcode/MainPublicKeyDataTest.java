package com.github.dedis.popstellar.model.qrcode;

import com.github.dedis.popstellar.model.network.serializer.JsonBase64DataSerializer;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainPublicKeyDataTest {

  private final Gson gson =
      new GsonBuilder()
          .registerTypeAdapter(PublicKey.class, new JsonBase64DataSerializer<>(PublicKey::new))
          .create();
  private final PublicKey pk = Base64DataUtils.generatePublicKey();

  @Test
  public void constructorAndGetterAreCoherent() {
    MainPublicKeyData data = new MainPublicKeyData(pk);
    assertEquals(pk, data.getPublicKey());
  }

  @Test
  public void extractDataTest() {
    String jsonFormat = gson.toJson(new MainPublicKeyData(pk));
    assertEquals(pk, MainPublicKeyData.extractFrom(gson, jsonFormat).getPublicKey());
  }
}