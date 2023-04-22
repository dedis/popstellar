package com.github.dedis.popstellar.utility;

import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;

import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.Assert.assertThrows;

public class MessageValidatorTest {
  private static final int DELTA_TIME = 100;
  private static final PublicKey ORGANIZER = Base64DataUtils.generatePublicKey();
  private static final String NAME = "lao name";
  private static final long CREATION = Instant.now().getEpochSecond();
  private static final String ID = Lao.generateLaoId(ORGANIZER, CREATION, NAME);

  @Test
  public void testCheckValidLaoId() {
    String invalid1 = "invalidID";
    String invalid2 = "A" + ID.substring(1);

    MessageValidator.verify().checkValidLaoId(ID, ORGANIZER, CREATION, NAME);
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkValidLaoId(invalid1, ORGANIZER, CREATION, NAME));
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkValidLaoId(invalid2, ORGANIZER, CREATION, NAME));
  }

  @Test
  public void testCheckValidTime() {
    long currentTime = Instant.now().getEpochSecond();
    long futureTime = currentTime + DELTA_TIME;

    MessageValidator.verify().checkValidTime(currentTime);
    assertThrows(
        IllegalArgumentException.class, () -> MessageValidator.verify().checkValidTime(futureTime));
    assertThrows(
        IllegalArgumentException.class, () -> MessageValidator.verify().checkValidTime(-1));
  }

  @Test
  public void testCheckValidOrderedTimes() {
    long currentTime = Instant.now().getEpochSecond();
    long futureTime = currentTime + DELTA_TIME;
    long pastTime = currentTime - DELTA_TIME;

    MessageValidator.verify().checkValidOrderedTimes(pastTime, currentTime);
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkValidOrderedTimes(futureTime, currentTime));
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkValidOrderedTimes(pastTime, futureTime));
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkValidOrderedTimes(-1, currentTime));
  }

  @Test
  public void testCheckBase64() {
    String validBase64 = Base64.getEncoder().encodeToString("test data".getBytes());
    String invalidBase64 = "This is not a valid Base64 string!";
    String field = "testField";

    MessageValidator.verify().checkBase64(validBase64, field);
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkBase64(invalidBase64, field));
    assertThrows(
        IllegalArgumentException.class, () -> MessageValidator.verify().checkBase64(null, field));
  }

  @Test
  public void testCheckStringNotEmpty() {
    String validString = "test string";
    String emptyString = "";
    String field = "testField";

    MessageValidator.verify().checkStringNotEmpty(validString, field);
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkStringNotEmpty(emptyString, field));
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkStringNotEmpty(null, field));
  }

  @Test
  public void testCheckListNotEmpty() {
    List<Integer> valid1 = Arrays.asList(1, 2, 3);
    List<String> valid2 = Arrays.asList("a", "b");
    List<String> invalid = new ArrayList<>();

    MessageValidator.verify().checkListNotEmpty(valid1);
    MessageValidator.verify().checkListNotEmpty(valid2);
    assertThrows(
        IllegalArgumentException.class, () -> MessageValidator.verify().checkListNotEmpty(invalid));
    assertThrows(
        IllegalArgumentException.class, () -> MessageValidator.verify().checkListNotEmpty(null));
  }

  @Test
  public void testCheckValidUrl() {
    MessageValidator.verify().checkValidUrl("http://example.com");
    MessageValidator.verify().checkValidUrl("https://example.com");
    MessageValidator.verify().checkValidUrl("ws://example.com");
    MessageValidator.verify().checkValidUrl("wss://10.0.2.2:8000/path");
    MessageValidator.verify().checkValidUrl("https://example.com/path/to/file.html");
    MessageValidator.verify().checkValidUrl("wss://example.com/path/to/file");

    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkValidUrl("Random String"));
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkValidUrl("example.com"));
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkValidUrl("http:example.com"));
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkValidUrl("://example.com"));
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().checkValidUrl("http://example."));
  }
}
