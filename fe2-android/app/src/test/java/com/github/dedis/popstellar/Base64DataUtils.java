package com.github.dedis.popstellar;

import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

import java.util.Base64;
import java.util.Random;

/**
 * This class provides pseudo random generators for base64 url data
 *
 * <p>This uses a fixed seed. So two test runs will have the same generated data This is important
 * when trying to extract the cause ofa failing test
 */
public class Base64DataUtils {

  private static final long SEED = 2022;
  private static final Random RANDOM = new Random(SEED);

  private static final int KEY_LENGTH = 32;
  private static final int MSG_ID_LENGTH = 32;
  private static final int SIGNATURE_LENGTH = 54;

  /** @return a pseudo randomly generated message id */
  public static MessageID generateMessageID() {
    byte[] msgId = new byte[MSG_ID_LENGTH];
    RANDOM.nextBytes(msgId);
    return new MessageID(Base64.getUrlEncoder().encodeToString(msgId));
  }

  /** @return a pseudo randomly generated message id different from the one provided */
  public static MessageID generateMessageIDOtherThan(MessageID other) {
    while (true) {
      MessageID msgId = generateMessageID();
      if (!msgId.equals(other)) return msgId;
    }
  }

  /** @return a pseudo randomly generated signature */
  public static Signature generateSignature() {
    byte[] signature = new byte[SIGNATURE_LENGTH];
    RANDOM.nextBytes(signature);
    return new Signature(signature);
  }

  /** @return a pseudo randomly generated valid Ed25519 keypair */
  public static KeyPair generateKeyPair() {
    byte[] privateKey = new byte[KEY_LENGTH];
    RANDOM.nextBytes(privateKey);

    Ed25519PrivateKeyParameters privateKeyParameters =
        new Ed25519PrivateKeyParameters(privateKey, 0);
    Ed25519PublicKeyParameters publicKeyParameters = privateKeyParameters.generatePublicKey();

    return new KeyPair(
        new PlainPrivateKey(privateKey), new PublicKey(publicKeyParameters.getEncoded()));
  }

  /** @return a pseudo randomly generated public key */
  public static PublicKey generatePublicKey() {
    byte[] key = new byte[KEY_LENGTH];
    RANDOM.nextBytes(key);
    return new PublicKey(key);
  }

  /** @return a pseudo randomly generated public key different from the one provided */
  public static PublicKey generatePublicKeyOtherThan(PublicKey other) {
    while (true) {
      PublicKey key = generatePublicKey();
      if (!key.equals(other)) return key;
    }
  }
}
