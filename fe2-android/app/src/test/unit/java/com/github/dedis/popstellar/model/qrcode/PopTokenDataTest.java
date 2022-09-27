package com.github.dedis.popstellar.model.qrcode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.Gson;

public class PopTokenDataTest {
  Gson gson = new Gson();

  @Test
  public void constructorAndGetterAreCoherent() {
    String string = "fooBar";
    PopTokenData data = new PopTokenData(string);
    assertEquals(string, data.getPopToken());
  }

  @Test
  public void extractDataTest() {
    String popToken = Base64DataUtils.generatePublicKey().getEncoded();
    String jsonFormat = gson.toJson(new PopTokenData(popToken));
    assertEquals(popToken, PopTokenData.extractFrom(gson, jsonFormat).getPopToken());
  }
}
