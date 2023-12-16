package com.github.dedis.popstellar.model.qrcode;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PoPCHAQRCodeTest {

  private static final long CREATION_TIME = Instant.now().getEpochSecond();
  private static final String LAO_NAME = "laoName";
  private static final KeyPair SENDER_KEY = generatePoPToken();
  private static final PublicKey SENDER = SENDER_KEY.publicKey;
  private static final String LAO_ID = Lao.generateLaoId(SENDER, CREATION_TIME, LAO_NAME);

  private static final String ADDRESS = "localhost:9100";
  private static final String RESPONSE_MODE = "query";
  private static final String CLIENT_ID = "WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU";
  private static final String NONCE =
      "frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA";
  private static final String STATE =
      "m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ";

  private static final String URL =
      createPoPCHAUrl(ADDRESS, RESPONSE_MODE, CLIENT_ID, LAO_ID, NONCE, STATE);

  private static final PoPCHAQRCode POPCHA_QR_CODE = new PoPCHAQRCode(URL, LAO_ID);

  @Test
  public void extractClientId() {
    assertEquals(CLIENT_ID, POPCHA_QR_CODE.clientId);
  }

  @Test
  public void extractNonce() {
    assertEquals(NONCE, POPCHA_QR_CODE.nonce);
  }

  @Test
  public void extractState() {
    assertEquals(STATE, POPCHA_QR_CODE.state);
  }

  @Test
  public void extractResponseMode() {
    assertEquals(RESPONSE_MODE, POPCHA_QR_CODE.responseMode);
  }

  @Test
  public void extractHost() {
    assertEquals(ADDRESS, POPCHA_QR_CODE.host);
  }

  @Test
  public void invalidUrl() {
    String invalidUrl = "http:/random.";
    assertThrows(IllegalArgumentException.class, () -> new PoPCHAQRCode(invalidUrl, LAO_ID));
  }

  @Test
  public void invalidResponseMode() {
    String invalidQueryUrl = createPoPCHAUrl(ADDRESS, "resp", CLIENT_ID, LAO_ID, NONCE, STATE);
    assertThrows(IllegalArgumentException.class, () -> new PoPCHAQRCode(invalidQueryUrl, LAO_ID));
  }

  @Test
  public void invalidLaoId() {
    String invalidLaoUrl =
        createPoPCHAUrl(ADDRESS, RESPONSE_MODE, CLIENT_ID, "lao_wrong", NONCE, STATE);
    assertThrows(IllegalArgumentException.class, () -> new PoPCHAQRCode(invalidLaoUrl, LAO_ID));
  }

  private static String createPoPCHAUrl(
      String address,
      String responseMode,
      String clientId,
      String laoId,
      String nonce,
      String state) {
    return "http://"
        + address
        + "/authorize?response_mode="
        + responseMode
        + "&response_type=id_token&client_id="
        + clientId
        + "&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint="
        + laoId
        + "&nonce="
        + nonce
        + "&state="
        + state;
  }
}
