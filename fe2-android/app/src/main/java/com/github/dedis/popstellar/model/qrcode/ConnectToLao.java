package com.github.dedis.popstellar.model.qrcode;

import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;

public class ConnectToLao {

  public final String server;
  public final String lao;

  private ConnectToLao(String server, String lao) {
    this.server = server;
    this.lao = lao;
  }

  public static ConnectToLao extractFrom(Gson mGson, Barcode barcode) {
    JsonUtils.verifyJson(JsonUtils.CONNECT_TO_LAO_SCHEMA, barcode.rawValue);
    return mGson.fromJson(barcode.rawValue, ConnectToLao.class);
  }
}
