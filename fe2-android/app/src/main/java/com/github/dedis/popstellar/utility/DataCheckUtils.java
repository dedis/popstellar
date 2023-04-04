package com.github.dedis.popstellar.utility;

import java.time.Instant;

import static com.github.dedis.popstellar.utility.Constants.BASE64_PATTERN;

public class DataCheckUtils {

  public static boolean isBase64(String input) {
    if (input == null || !BASE64_PATTERN.matcher(input).matches()) {
      return false;
    }
    return true;
  }

  public static boolean isInFuture(long creationTime) {
    if (creationTime > Instant.now().getEpochSecond()) {
      return true;
    }
    return false;
  }
}
