package be.utils;

import be.model.KeyPair;
import be.model.Lao;
import be.model.RollCall;
import common.utils.Base64Utils;

import java.time.Instant;

/** Class used to create random ids, names, signatures for tests */
public class RandomUtils {
  private static final long SEED = 2023;
  private static final java.util.Random RANDOM = new java.util.Random(SEED);
  private static final int SIGNATURE_LENGTH = 54;
  private static final String NAME = "some name";
  private static final String ID = "some id";
  private static final long CREATION = Instant.now().getEpochSecond();

  /** Some parameters to create valid random names */
  private static final String VALID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.@*#!?_+=-(){}[]%&$^~|:;,<>/ ";
  private static final int MIN_NAME_LENGTH = 1;
  private static final int MAX_NAME_LENGTH = 30;

  /** @return a pseudo randomly generated signature */
  public static String generateSignature(){
    byte[] signature = new byte[SIGNATURE_LENGTH];
    RANDOM.nextBytes(signature);
    return Base64Utils.encode(signature);
  }

  /** @return generates a random valid Lao id */
  public static String generateLaoId(){
    KeyPair randomKeyPair = new KeyPair();
    return Lao.generateLaoId(randomKeyPair.getPublicKey(), CREATION, NAME);
  }

  /** @return generate a random create roll call id */
  public static String generateCreateRollCallId(){
    String randomLaoId = generateLaoId();
    return RollCall.generateCreateRollCallId(randomLaoId, CREATION, NAME);
  }

  /** @return generate a random open roll call id */
  public static String generateOpenRollCallId(){
    String randomLaoId = generateLaoId();
    return RollCall.generateOpenRollCallId(randomLaoId, ID, 1);
  }

  /** @return generate a random close roll call id */
  public static String generateCloseRollCallId(){
    String randomLaoId = generateLaoId();
    return RollCall.generateCloseRollCallId(randomLaoId, ID, 1);
  }

  /** @return generate a random valid name for a lao or roll call */
  public static String generateRandomName() {
    int length = MIN_NAME_LENGTH + RANDOM.nextInt(MAX_NAME_LENGTH - MIN_NAME_LENGTH + 1);
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < length; i++) {
      int index = RANDOM.nextInt(VALID_CHARS.length());
      builder.append(VALID_CHARS.charAt(index));
    }
    return builder.toString();
  }
}
