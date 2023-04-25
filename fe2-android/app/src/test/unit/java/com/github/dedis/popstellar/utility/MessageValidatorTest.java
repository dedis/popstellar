package com.github.dedis.popstellar.utility;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
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

    MessageValidator.verify().validLaoId(ID, ORGANIZER, CREATION, NAME);
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().validLaoId(invalid1, ORGANIZER, CREATION, NAME));
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().validLaoId(invalid2, ORGANIZER, CREATION, NAME));
  }

  @Test
  public void testCheckValidPastTimes() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    long currentTime = Instant.now().getEpochSecond();
    // time that is too far in the past to be considered valid
    long pastTime = currentTime - validator.VALID_DELAY - 1;
    long futureTime = currentTime + DELTA_TIME;

    validator.validPastTimes(currentTime);
    assertThrows(IllegalArgumentException.class, () -> validator.validPastTimes(futureTime));
    assertThrows(IllegalArgumentException.class, () -> validator.validPastTimes(pastTime));
  }

  @Test
  public void testCheckOrderedTimes() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    long currentTime = Instant.now().getEpochSecond();
    long futureTime = currentTime + DELTA_TIME;
    long pastTime = currentTime - DELTA_TIME;

    validator.orderedTimes(pastTime, currentTime, futureTime);
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.orderedTimes(pastTime, futureTime, currentTime));
  }

  @Test
  public void testCheckBase64() {
    String validBase64 = Base64.getEncoder().encodeToString("test data".getBytes());
    String invalidBase64 = "This is not a valid Base64 string!";
    String field = "testField";

    MessageValidator.verify().isBase64(validBase64, field);
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().isBase64(invalidBase64, field));
    assertThrows(
        IllegalArgumentException.class, () -> MessageValidator.verify().isBase64(null, field));
  }

  @Test
  public void testCheckStringNotEmpty() {
    String validString = "test string";
    String emptyString = "";
    String field = "testField";

    MessageValidator.verify().stringNotEmpty(validString, field);
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().stringNotEmpty(emptyString, field));
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageValidator.verify().stringNotEmpty(null, field));
  }

  @Test
  public void testCheckListNotEmpty() {
    List<Integer> valid1 = Arrays.asList(1, 2, 3);
    List<String> valid2 = Arrays.asList("a", "b");
    List<String> invalid = new ArrayList<>();

    MessageValidator.verify().listNotEmpty(valid1);
    MessageValidator.verify().listNotEmpty(valid2);
    assertThrows(
        IllegalArgumentException.class, () -> MessageValidator.verify().listNotEmpty(invalid));
    assertThrows(
        IllegalArgumentException.class, () -> MessageValidator.verify().listNotEmpty(null));
  }

  @Test
  public void testCheckNoListDuplicates() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();

    ElectionQuestion.Question q1 =
        new ElectionQuestion.Question(
            "Which is the best ?", "Plurality", Arrays.asList("Option a", "Option b"), false);
    ElectionQuestion.Question q2 =
        new ElectionQuestion.Question(
            "Which is the best ?", "Plurality", Arrays.asList("Option a", "Option b"), false);
    ElectionQuestion.Question q3 =
        new ElectionQuestion.Question(
            "Not the same question ?", "Plurality", Arrays.asList("Option c", "Option d"), true);

    List<Integer> valid1 = Arrays.asList(1, 2, 3);
    List<ElectionQuestion.Question> valid2 = Arrays.asList(q1, q3);
    List<String> valid3 = new ArrayList<>();

    List<Integer> invalid1 = Arrays.asList(1, 2, 2);
    List<ElectionQuestion.Question> invalid2 = Arrays.asList(q1, q2);

    validator.noListDuplicates(valid1);
    validator.noListDuplicates(valid2);
    validator.noListDuplicates(valid3);

    assertThrows(IllegalArgumentException.class, () -> validator.noListDuplicates(invalid1));
    assertThrows(IllegalArgumentException.class, () -> validator.noListDuplicates(invalid2));
  }
}
