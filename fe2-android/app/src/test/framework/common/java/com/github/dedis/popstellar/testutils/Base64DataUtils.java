package com.github.dedis.popstellar.testutils;

import com.github.dedis.popstellar.model.objects.security.*;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

import java.util.Base64;
import java.util.Random;

/**
 * This class provides pseudo random generators for base64 url data
 *
 * <p>This uses a fixed seed. So two test runs will have the same generated data This is important
 * when trying to extract the cause ofa failing test
 *
 * <p>Do not use this outside of tests, this is extremely unsecure
 */
public class Base64DataUtils {

  private static final long SEED = 2022;
  private static final Random RANDOM = new Random(SEED);

  private static final int KEY_LENGTH = 32;
  private static final int MSG_ID_LENGTH = 32;
  private static final int SIGNATURE_LENGTH = 54;

  private Base64DataUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * @return a pseudo randomly generated message id
   */
  public static MessageID generateMessageID() {
    byte[] msgId = new byte[MSG_ID_LENGTH];
    RANDOM.nextBytes(msgId);
    return new MessageID(Base64.getUrlEncoder().encodeToString(msgId));
  }

  /**
   * @return a pseudo randomly generated message id different from the one provided
   */
  public static MessageID generateMessageIDOtherThan(MessageID other) {
    while (true) {
      MessageID msgId = generateMessageID();
      if (!msgId.equals(other)) return msgId;
    }
  }

  /**
   * @return a pseudo randomly generated signature
   */
  public static Signature generateSignature() {
    byte[] signature = new byte[SIGNATURE_LENGTH];
    RANDOM.nextBytes(signature);
    return new Signature(signature);
  }

  /**
   * @return a pseudo randomly generated valid Ed25519 keypair
   */
  public static KeyPair generateKeyPair() {
    return generatePoPToken();
  }

  /**
   * @return a pseudo randomly generated valid Ed25519 keypair
   */
  public static PoPToken generatePoPToken() {
    byte[] privateKey = new byte[KEY_LENGTH];
    RANDOM.nextBytes(privateKey);

    Ed25519PrivateKeyParameters privateKeyParameters =
        new Ed25519PrivateKeyParameters(privateKey, 0);
    Ed25519PublicKeyParameters publicKeyParameters = privateKeyParameters.generatePublicKey();

    return new PoPToken(privateKey, publicKeyParameters.getEncoded());
  }

  /**
   * @return a pseudo randomly generated public key
   */
  public static PublicKey generatePublicKey() {
    byte[] key = new byte[KEY_LENGTH];
    RANDOM.nextBytes(key);
    return new PublicKey(key);
  }

  /**
   * @return a pseudo randomly generated public key different from the one provided
   */
  public static PublicKey generatePublicKeyOtherThan(PublicKey other) {
    while (true) {
      PublicKey key = generatePublicKey();
      if (!key.equals(other)) return key;
    }
  }

  /**
   * @return a pseudo randomly generated base 64 string
   */
  public static String generateRandomBase64String() {
    byte[] randomBytes = new byte[KEY_LENGTH];
    RANDOM.nextBytes(randomBytes);
    return Base64.getUrlEncoder().encodeToString(randomBytes);
  }
}
