package com.github.dedis.popstellar.model.qrcode;

import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.Gson;

import org.junit.Test;

public class MainPublicKeyDataTest {

  Gson gson = new Gson();
  PublicKey pk = Base64DataUtils.generatePublicKey();

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
