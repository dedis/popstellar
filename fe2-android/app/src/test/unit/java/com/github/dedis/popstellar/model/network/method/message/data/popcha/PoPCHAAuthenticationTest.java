package com.github.dedis.popstellar.model.network.method.message.data.popcha;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.utility.security.Hash;
import org.junit.Test;

public class PoPCHAAuthenticationTest {

  private static final String CLIENT_ID = Hash.hash("clientID");
  private static final String NONCE = Hash.hash("random");
  private static final Base64URLData IDENTIFIER = new Base64URLData("identifier");
  private static final Base64URLData IDENTIFIER_PROOF = new Base64URLData("identifier-proof");
  private static final String STATE = null;
  private static final String RESPONSE_MODE = "id_token";
  private static final String ADDRESS = "localhost:9100";

  private static final PoPCHAAuthentication POPCHA_AUTHENTICATION =
      new PoPCHAAuthentication(
          CLIENT_ID, NONCE, IDENTIFIER, IDENTIFIER_PROOF, ADDRESS, STATE, RESPONSE_MODE);

  @Test
  public void getClientId() {
    assertEquals(CLIENT_ID, POPCHA_AUTHENTICATION.clientId);
  }

  @Test
  public void getNonce() {
    assertEquals(NONCE, POPCHA_AUTHENTICATION.nonce);
  }

  @Test
  public void getIdentifier() {
    assertEquals(IDENTIFIER, POPCHA_AUTHENTICATION.identifier);
  }

  @Test
  public void getIdentifierProof() {
    assertEquals(IDENTIFIER_PROOF, POPCHA_AUTHENTICATION.identifierProof);
  }

  @Test
  public void getState() {
    assertEquals(STATE, POPCHA_AUTHENTICATION.state);
  }

  @Test
  public void getResponseMode() {
    assertEquals(RESPONSE_MODE, POPCHA_AUTHENTICATION.responseMode);
  }

  @Test
  public void getPopchaAddress() {
    assertEquals(ADDRESS, POPCHA_AUTHENTICATION.popchaAddress);
  }

  @Test
  public void getObject() {
    assertEquals(Objects.POPCHA.getObject(), POPCHA_AUTHENTICATION.getObject());
  }

  @Test
  public void getAction() {
    assertEquals(Action.AUTH.getAction(), POPCHA_AUTHENTICATION.getAction());
  }

  @Test
  public void testEquals() {
    assertEquals(POPCHA_AUTHENTICATION, POPCHA_AUTHENTICATION);
    assertNotEquals(null, POPCHA_AUTHENTICATION);

    PoPCHAAuthentication popCHAAuthentication1 =
        new PoPCHAAuthentication(
            CLIENT_ID, NONCE, IDENTIFIER, IDENTIFIER_PROOF, ADDRESS, STATE, RESPONSE_MODE);
    assertEquals(POPCHA_AUTHENTICATION, popCHAAuthentication1);
  }

  @Test
  public void testHashCode() {
    assertEquals(
        java.util.Objects.hash(
            CLIENT_ID, NONCE, IDENTIFIER, IDENTIFIER_PROOF, STATE, RESPONSE_MODE, ADDRESS),
        POPCHA_AUTHENTICATION.hashCode());
  }

  @Test
  public void testToString() {
    String expected =
        String.format(
            "PoPCHAAuthentication{clientId='%s', nonce='%s', identifier='%s', identifierProof='%s', state='%s', responseMode='%s', popchaAddress='%s'}",
            CLIENT_ID, NONCE, IDENTIFIER, IDENTIFIER_PROOF, STATE, RESPONSE_MODE, ADDRESS);
    assertEquals(expected, POPCHA_AUTHENTICATION.toString());
  }
}
