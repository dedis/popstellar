package be.utils;

import java.time.Instant;

public class Random {

    /**
     * @return the converted byte array
     */
    public static String generateSenderPk(){
      return "";
    }

    /**
     * @return the converted byte array
     */
    public static String generatePrivateKeyHex(){
      return "";
    }

    /**
     * @return the converted byte array
     */
    public static String generateSignature(){
      return "";
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
