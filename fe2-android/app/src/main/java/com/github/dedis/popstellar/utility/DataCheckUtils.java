package com.github.dedis.popstellar.utility;

import java.time.Instant;
import java.util.regex.Pattern;

public class DataCheckUtils {

  /** URL-safe base64 pattern */
  public static final Pattern BASE64_PATTERN =
      Pattern.compile("^(?:[A-Za-z0-9-_]{4})*(?:[A-Za-z0-9-_]{2}==|[A-Za-z0-9-_]{3}=)?$");

  /**
   * Helper method to check that a string is not empty.
   *
   * @param input the string to check
   * @param field name of the field (to print in case of error)
   * @throws IllegalArgumentException if the string is empty or null
   */
  public static void checkStringNotEmpty(String input, String field) {
    if (input == null || input.isEmpty()) {
      throw new IllegalArgumentException(field + " cannot be empty");
    }
  }

  /**
   * Helper method to check that a string is a valid URL-safe base64 encoding.
   *
   * @param input the string to check
   * @param field name of the field (to print in case of error)
   * @throws IllegalArgumentException if the string is not a URL-safe base64 encoding
   */
  public static void checkBase64(String input, String field) {
    if (input == null || !BASE64_PATTERN.matcher(input).matches()) {
      throw new IllegalArgumentException(field + " must be a base 64 encoded string");
    }
  }

  /**
   * Helper method to check that times in a Data are valid. The time values provided are assumed to
   * be in Unix epoch time (UTC). Checks that time is valid and that the first time comes before the
   * second time.
   *
   * @param beforeTime before time
   * @param afterTime after time
   * @throws IllegalArgumentException if a time is invalid or if the first time comes after the
   *     second
   */
  public static void checkValidOrderedTimes(long beforeTime, long afterTime) {
    checkValidTime(beforeTime);
    checkValidTime(afterTime);
    if (afterTime < beforeTime) {
      throw new IllegalArgumentException("Last modified time cannot be before creation time");
    }
  }

  /**
   * Helper method to check that time in a Data is valid. The time value provided is assumed to be
   * in Unix epoch time (UTC). Checks that: 1) time is not negative (this is already checked at
   * deserialization), 2) time is not in the future
   *
   * @param time time to check
   * @throws IllegalArgumentException if time is invalid
   */
  public static void checkValidTime(long time) {
    if (time < 0) {
      throw new IllegalArgumentException("Time cannot be negative");
    } else if (isInFuture(time)) {
      throw new IllegalArgumentException("Time cannot be in the future");
    }
  }

  private static boolean isInFuture(long creationTime) {
    return (creationTime > Instant.now().getEpochSecond());
  }
}
