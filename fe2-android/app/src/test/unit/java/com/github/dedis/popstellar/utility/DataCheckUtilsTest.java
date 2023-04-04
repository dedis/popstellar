package com.github.dedis.popstellar.utility;

import org.junit.Test;

import java.time.Instant;
import java.util.Base64;

import static org.junit.Assert.assertThrows;

public class DataCheckUtilsTest {
  private static final int TIME = 100;

  @Test
  public void testCheckBase64() {
    String validBase64 = Base64.getEncoder().encodeToString("test data".getBytes());
    String invalidBase64 = "This is not a valid Base64 string!";
    String field = "testField";

    DataCheckUtils.checkBase64(validBase64, field);
    assertThrows(
        IllegalArgumentException.class, () -> DataCheckUtils.checkBase64(invalidBase64, field));
    assertThrows(IllegalArgumentException.class, () -> DataCheckUtils.checkBase64(null, field));
  }

  @Test
  public void testCheckValidOrderedTimes() {
    long currentTime = Instant.now().getEpochSecond();
    long futureTime = currentTime + TIME;
    long pastTime = currentTime - TIME;

    DataCheckUtils.checkValidOrderedTimes(pastTime, currentTime);
    assertThrows(
        IllegalArgumentException.class,
        () -> DataCheckUtils.checkValidOrderedTimes(futureTime, currentTime));
    assertThrows(
        IllegalArgumentException.class,
        () -> DataCheckUtils.checkValidOrderedTimes(pastTime, futureTime));
    assertThrows(
        IllegalArgumentException.class,
        () -> DataCheckUtils.checkValidOrderedTimes(-1, currentTime));
  }

  @Test
  public void testCheckValidTime() {
    long currentTime = Instant.now().getEpochSecond();
    long futureTime = currentTime + TIME;

    DataCheckUtils.checkValidTime(currentTime);
    assertThrows(IllegalArgumentException.class, () -> DataCheckUtils.checkValidTime(futureTime));
    assertThrows(IllegalArgumentException.class, () -> DataCheckUtils.checkValidTime(-1));
  }
}
