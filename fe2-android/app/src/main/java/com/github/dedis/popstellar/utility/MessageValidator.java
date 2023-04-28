package com.github.dedis.popstellar.utility;

import com.github.dedis.popstellar.model.network.method.message.data.election.Vote;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import java.time.Instant;
import java.util.*;
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

    // Defines how old messages can be to be considered valid, keeping it non-restrictive here for
    // now
    public static final long VALID_DELAY = 100000000;
    private final long CURRENT_TIME = Instant.now().getEpochSecond();
    private final long VALID_PAST_TIME = CURRENT_TIME - VALID_DELAY;

    /**
     * Helper method to check that a LAO id is valid.
     *
     * @param organizer the lao organizer
     * @param creation the lao creation time
     * @param name the lao name
     * @throws IllegalArgumentException if the id is invalid
     */
    public MessageValidatorBuilder validLaoId(
        String id, PublicKey organizer, long creation, String name) {
      // If any of the arguments are empty or null this throws an exception
      if (!id.equals(Lao.generateLaoId(organizer, creation, name))) {
        throw new IllegalArgumentException("CreateLao id must be Hash(organizer||creation||name)");
      }
      return this;
    }

    /**
     * Helper method to check that times are reasonably recent.The time values provided are assumed
     * to be in Unix epoch time (UTC).
     *
     * @param times time values to be checked
     * @throws IllegalArgumentException if times are too far in the past
     */
    public MessageValidatorBuilder validPastTimes(Long... times) {
      for (long time : times) {
        if (time < VALID_PAST_TIME) {
          throw new IllegalArgumentException("Time cannot be too far in the past");
        }
        if (time > CURRENT_TIME) {
          throw new IllegalArgumentException("Time cannot be in the future");
        }
      }
      return this;
    }

    /**
     * Helper method to check that times in a Data are ordered. The time values provided are assumed
     * to be in Unix epoch time (UTC).
     *
     * @param times time values to be checked
     * @throws IllegalArgumentException if the times are not in ascending order second
     */
    public MessageValidatorBuilder orderedTimes(Long... times) {
      for (int i = 0; i < times.length - 1; i++) {
        if (times[i + 1] < times[i]) {
          throw new IllegalArgumentException("Times must be in ascending order");
        }
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
    public MessageValidatorBuilder isBase64(String input, String field) {
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
    public MessageValidatorBuilder stringNotEmpty(String input, String field) {
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
    public MessageValidatorBuilder listNotEmpty(List<?> list) {
      if (list == null || list.isEmpty()) {
        throw new IllegalArgumentException("List cannot be empty");
      }
      return this;
    }

    /**
     * Helper method to check that a list has no duplicates. This method relies on hashCode for
     * equality comparison.
     *
     * @param list the list to check
     * @throws IllegalArgumentException if there are duplicates
     */
    public MessageValidatorBuilder noListDuplicates(List<?> list) {
      Set<?> uniqueElements = new HashSet<>(list);

      if (uniqueElements.size() != list.size()) {
        throw new IllegalArgumentException("List has duplicates");
      }
      return this;
    }

    /**
     * Helper method to check that a list of votes has no duplicates and their ids are base 64.
     *
     * @param votes the list to check
     * @throws IllegalArgumentException if the list has invalid votes
     */
    public MessageValidatorBuilder validVotes(List<? extends Vote> votes) {
      noListDuplicates(votes);

      for (Vote vote : votes) {
        isBase64(vote.getQuestionId(), "question id");
        isBase64(vote.getId(), "vote id");
      }
      return this;
    }

    public MessageValidatorBuilder checkValidUrl(String input) {
      if (input == null || !URL_PATTERN.matcher(input).matches()) {
        throw new IllegalArgumentException("Input is not a url");
      }
      return this;
    }
  }
}
