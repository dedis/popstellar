package be.utils;

import java.time.Instant;
import common.utils.Base64Utils;


public class Random {
  private static final long SEED = 2023;
  private static final java.util.Random RANDOM = new java.util.Random(SEED);
  private static final int SIGNATURE_LENGTH = 54;

  /**
   * @return a pseudo randomly generated signature
   */
  public static String generateSignature(){
    byte[] signature = new byte[SIGNATURE_LENGTH];
    RANDOM.nextBytes(signature);
    return Base64Utils.encode(signature);
  }

  /**
   * @return generate a random valid Lao id
   */
  public static String generateLaoId(){
    KeyPair keyPair = new KeyPair();

    return Lao.generateLaoId(keyPair.getPublicKey(), Instant.now().getEpochSecond(), "some name");
  }

  /**
   * @return generate a random valid public key
   */
    public static String generatePublicKey(){
    KeyPair keyPair = new KeyPair();

    return keyPair.getPublicKey();
  }

}
