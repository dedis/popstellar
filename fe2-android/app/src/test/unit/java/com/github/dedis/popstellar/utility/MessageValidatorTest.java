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
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    String invalid1 = "invalidID";
    String invalid2 = "A" + ID.substring(1);

    validator.checkValidLaoId(ID, ORGANIZER, CREATION, NAME);
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.checkValidLaoId(invalid1, ORGANIZER, CREATION, NAME));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.checkValidLaoId(invalid2, ORGANIZER, CREATION, NAME));
  }

  @Test
  public void testCheckValidTime() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    long currentTime = Instant.now().getEpochSecond();
    long futureTime = currentTime + DELTA_TIME;

    validator.checkValidTime(currentTime);
    assertThrows(IllegalArgumentException.class, () -> validator.checkValidTime(futureTime));
    assertThrows(IllegalArgumentException.class, () -> validator.checkValidTime(-1));
  }

  @Test
  public void testCheckValidOrderedTimes() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    long currentTime = Instant.now().getEpochSecond();
    long futureTime = currentTime + DELTA_TIME;
    long pastTime = currentTime - DELTA_TIME;

    validator.checkValidOrderedTimes(pastTime, currentTime);
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.checkValidOrderedTimes(futureTime, currentTime));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.checkValidOrderedTimes(pastTime, futureTime));
    assertThrows(
        IllegalArgumentException.class, () -> validator.checkValidOrderedTimes(-1, currentTime));
  }

  @Test
  public void testCheckBase64() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    String validBase64 = Base64.getEncoder().encodeToString("test data".getBytes());
    String invalidBase64 = "This is not a valid Base64 string!";
    String field = "testField";

    validator.checkBase64(validBase64, field);
    assertThrows(IllegalArgumentException.class, () -> validator.checkBase64(invalidBase64, field));
    assertThrows(IllegalArgumentException.class, () -> validator.checkBase64(null, field));
  }

  @Test
  public void testCheckStringNotEmpty() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    String validString = "test string";
    String emptyString = "";
    String field = "testField";

    validator.checkStringNotEmpty(validString, field);
    assertThrows(
        IllegalArgumentException.class, () -> validator.checkStringNotEmpty(emptyString, field));
    assertThrows(IllegalArgumentException.class, () -> validator.checkStringNotEmpty(null, field));
  }

  @Test
  public void testCheckListNotEmpty() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    List<Integer> valid1 = Arrays.asList(1, 2, 3);
    List<String> valid2 = Arrays.asList("a", "b");
    List<String> invalid = new ArrayList<>();

    validator.checkListNotEmpty(valid1);
    validator.checkListNotEmpty(valid2);
    assertThrows(IllegalArgumentException.class, () -> validator.checkListNotEmpty(invalid));
    assertThrows(IllegalArgumentException.class, () -> validator.checkListNotEmpty(null));
  }

  @Test
  public void testCheckValidUrl() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();

    validator.checkValidUrl("http://example.com");
    validator.checkValidUrl("https://example.com");
    validator.checkValidUrl("ws://example.com");
    validator.checkValidUrl("wss://10.0.2.2:8000/path");
    validator.checkValidUrl("https://example.com/path/to/file.html");
    validator.checkValidUrl("wss://example.com/path/to/file");

    assertThrows(IllegalArgumentException.class, () -> validator.checkValidUrl("Random String"));
    assertThrows(IllegalArgumentException.class, () -> validator.checkValidUrl("example.com"));
    assertThrows(IllegalArgumentException.class, () -> validator.checkValidUrl("http:example.com"));
    assertThrows(IllegalArgumentException.class, () -> validator.checkValidUrl("://example.com"));
    assertThrows(IllegalArgumentException.class, () -> validator.checkValidUrl("http://example."));
  }
}
