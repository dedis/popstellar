package com.github.dedis.popstellar.utility;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/** Helper class to verify the validity of Data objects at their creation. */
public abstract class MessageValidator {

  /** URL-safe base64 pattern */
  private static final Pattern BASE64_PATTERN =
      Pattern.compile("^(?:[A-Za-z0-9-_]{4})*(?:[A-Za-z0-9-_]{2}==|[A-Za-z0-9-_]{3}=)?$");

  private static final Pattern URL_PATTERN =
      Pattern.compile("\\b(?:http|ws)s?:\\/\\/\\S*[^\\s.\"]");

  /** Prevent instantiations */
  private MessageValidator() {}

  public static MessageValidatorBuilder verify() {
    return new MessageValidatorBuilder();
  }

  public static class MessageValidatorBuilder {

    /**
     * Helper method to check that a LAO id is valid.
     *
     * @param organizer the lao organizer
     * @param creation the lao creation time
     * @param name the lao name
     * @throws IllegalArgumentException if the id is invalid
     */
    public MessageValidatorBuilder checkValidLaoId(
        String id, PublicKey organizer, long creation, String name) {
      // If any of the arguments are empty or null this throws an exception
      if (!id.equals(Lao.generateLaoId(organizer, creation, name))) {
        throw new IllegalArgumentException("CreateLao id must be Hash(organizer||creation||name)");
      }
      return this;
    }

    /**
     * Helper method to check that time in a Data is valid. The time value provided is assumed to be
     * in Unix epoch time (UTC). Checks that: 1) time is not negative 2) time is not in the future
     *
     * @param time time to check
     * @throws IllegalArgumentException if time is invalid
     */
    public MessageValidatorBuilder checkValidTime(long time) {
      if (time < 0) {
        throw new IllegalArgumentException("Time cannot be negative");
      } else if (isInFuture(time)) {
        throw new IllegalArgumentException("Time cannot be in the future");
      }
      return this;
    }

    /**
     * Helper method to check that times in a Data are valid. The time values provided are assumed
     * to be in Unix epoch time (UTC). Checks that time is valid and that the first time comes
     * before the second time.
     *
     * @param beforeTime before time
     * @param afterTime after time
     * @throws IllegalArgumentException if a time is invalid or if the first time comes after the
     *     second
     */
    public MessageValidatorBuilder checkValidOrderedTimes(long beforeTime, long afterTime) {
      checkValidTime(beforeTime);
      checkValidTime(afterTime);
      if (afterTime < beforeTime) {
        throw new IllegalArgumentException("Last modified time cannot be before creation time");
      }
      return this;
    }

    /**
     * Helper method to check that a string is a valid URL-safe base64 encoding.
     *
     * @param input the string to check
     * @param field name of the field (to print in case of error)
     * @throws IllegalArgumentException if the string is not a URL-safe base64 encoding
     */
    public MessageValidatorBuilder checkBase64(String input, String field) {
      if (input == null || !BASE64_PATTERN.matcher(input).matches()) {
        throw new IllegalArgumentException(field + " must be a base 64 encoded string");
      }
      return this;
    }

    /**
     * Helper method to check that a string is not empty.
     *
     * @param input the string to check
     * @param field name of the field (to print in case of error)
     * @throws IllegalArgumentException if the string is empty or null
     */
    public MessageValidatorBuilder checkStringNotEmpty(String input, String field) {
      if (input == null || input.isEmpty()) {
        throw new IllegalArgumentException(field + " cannot be empty");
      }
      return this;
    }

    /**
     * Helper method to check that a list is not empty.
     *
     * @param list the list to check
     * @throws IllegalArgumentException if the string is empty or null
     */
    public MessageValidatorBuilder checkListNotEmpty(List<?> list) {
      if (list == null || list.isEmpty()) {
        throw new IllegalArgumentException("List cannot be empty");
      }
      return this;
    }

    public MessageValidatorBuilder checkValidUrl(String input) {
      if (input == null || !URL_PATTERN.matcher(input).matches()) {
        throw new IllegalArgumentException("Input is not a url");
      }
      return this;
    }

    private static boolean isInFuture(long creationTime) {
      return (creationTime > Instant.now().getEpochSecond());
    }
  }
}
