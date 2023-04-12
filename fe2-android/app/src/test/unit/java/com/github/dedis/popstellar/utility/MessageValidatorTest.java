package com.github.dedis.popstellar.utility;

import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.util.Base64;
import org.junit.Test;

public class MessageValidatorTest {
  private static final int DELTA_TIME = 100;

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
  public void testCheckValidTime() {
    long currentTime = Instant.now().getEpochSecond();
    long futureTime = currentTime + DELTA_TIME;

    MessageValidator.verify().checkValidTime(currentTime);
    assertThrows(
        IllegalArgumentException.class, () -> MessageValidator.verify().checkValidTime(futureTime));
    assertThrows(
        IllegalArgumentException.class, () -> MessageValidator.verify().checkValidTime(-1));
  }
}
