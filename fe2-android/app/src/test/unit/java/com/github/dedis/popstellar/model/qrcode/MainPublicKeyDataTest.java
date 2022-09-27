package com.github.dedis.popstellar.model.qrcode;

import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.Gson;

import org.junit.Test;

public class MainPublicKeyDataTest {

  Gson gson = new Gson();

  @Test
  public void constructorAndGetterAreCoherent() {
    String string = "fooBar";
    MainPublicKeyData data = new MainPublicKeyData(string);
    assertEquals(string, data.getPublicKey());
  }

  @Test
  public void extractDataTest() {
    String pk = Base64DataUtils.generatePublicKey().getEncoded();
    String jsonFormat = gson.toJson(new MainPublicKeyData(pk));
    assertEquals(pk, MainPublicKeyData.extractFrom(gson, jsonFormat).getPublicKey());
  }
}
