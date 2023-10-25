package com.github.dedis.popstellar.model.qrcode;

import android.net.Uri;
import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.utility.MessageValidator;
import java.util.Locale;

@Immutable
public class PoPCHAQRCode {

  public static final String CLIENT_ID = "client_id";
  public static final String NONCE = "nonce";
  public static final String REDIRECT_URI = "redirect_uri";
  public static final String STATE = "state";
  public static final String RESPONSE_TYPE = "response_type";
  public static final String RESPONSE_MODE = "response_mode";
  public static final String LOGIN_HINT = "login_hint";

  private final String clientId;
  private final String nonce;
  private final String state;
  private final String responseMode;
  private final String host;

  public PoPCHAQRCode(String data, String laoId) throws IllegalArgumentException {
    MessageValidator.verify().isValidPoPCHAUrl(data, laoId);

    Uri uri = Uri.parse(data);
    clientId = uri.getQueryParameter(CLIENT_ID);
    nonce = uri.getQueryParameter(NONCE);
    state = uri.getQueryParameter(STATE);
    responseMode = uri.getQueryParameter(RESPONSE_MODE);
    final int port = uri.getPort();
    host =
        String.format(
            "%s%s", uri.getHost(), port == -1 ? "" : String.format(Locale.ENGLISH, ":%d", port));
  }

  public String getClientId() {
    return clientId;
  }

  public String getNonce() {
    return nonce;
  }

  public String getState() {
    return state;
  }

  public String getResponseMode() {
    return responseMode;
  }

  public String getHost() {
    return host;
  }

  @Override
  public String toString() {
    return "PoPCHAQRCode{"
        + "clientId='"
        + clientId
        + '\''
        + ", nonce='"
        + nonce
        + '\''
        + ", state='"
        + state
        + '\''
        + ", responseMode='"
        + responseMode
        + '\''
        + ", host='"
        + host
        + '\''
        + '}';
  }
}
