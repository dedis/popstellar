package com.github.dedis.popstellar.utility.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/** SHA256 Hashing Class */
public class Hash {

  private static final Logger logger = LogManager.getLogger(Hash.class);

  /**
   * Hash some objects using SHA256. Concatenate the object's string representation following the
   * protocol's directive. Then hash the obtained string
   *
   * @param strs : the strings to hash
   * @return the hashed data or null if failed to hash
   * @throws IllegalArgumentException if the data is null or empty
   * @throws UnsupportedOperationException if SHA-256 MessageDigest is unavailable
   */
  public static String hash(String... strs) {
    if (strs == null || strs.length == 0) {
      throw new IllegalArgumentException("cannot hash an empty/null array");
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (String str : strs) {
        if (str == null || str.length() == 0) {
          throw new IllegalArgumentException("cannot hash an empty/null string");
        }
        byte[] buf = str.getBytes(StandardCharsets.UTF_8);
        String length = Integer.toString(buf.length);
        digest.update(length.getBytes(StandardCharsets.UTF_8));
        digest.update(buf);
      }
      byte[] digestBuf = digest.digest();
      return Base64.getUrlEncoder().encodeToString(digestBuf);
    } catch (NoSuchAlgorithmException e) {
      logger.error("failed to hash", e);
      throw new UnsupportedOperationException("failed to retrieve SHA-256 instance", e);
    }
  }
}
