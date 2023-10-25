package com.github.dedis.popstellar.utility;

import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MessageValidatorTest {
  private static final long DELTA_TIME =
      MessageValidator.MessageValidatorBuilder.VALID_FUTURE_DELAY + 100;

  // LAO constants
  private static final PublicKey ORGANIZER = Base64DataUtils.generatePublicKey();
  private static final String NAME = "lao name";
  private static final long CREATION = Instant.now().getEpochSecond();
  private static final String LAO_ID = Lao.generateLaoId(ORGANIZER, CREATION, NAME);

  // Election constants
  private static final String ELECTION_ID =
      Election.generateElectionSetupId(LAO_ID, CREATION, "election name");
  private final String QUESTION_ID1 =
      Election.generateElectionQuestionId(ELECTION_ID, "Question 1");
  private final String QUESTION_ID2 =
      Election.generateElectionQuestionId(ELECTION_ID, "Question 2");

  @Test
  public void testValidLaoId() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    String invalid1 = "invalidID";
    String invalid2 =
        Lao.generateLaoId(Base64DataUtils.generatePublicKeyOtherThan(ORGANIZER), 0, "name");

    validator.validLaoId(LAO_ID, ORGANIZER, CREATION, NAME);
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validLaoId(invalid1, ORGANIZER, CREATION, NAME));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validLaoId(invalid2, ORGANIZER, CREATION, NAME));
  }

  @Test
  public void testValidPastTimes() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    long currentTime = Instant.now().getEpochSecond();
    // time that is too far in the past to be considered valid
    long pastTime = currentTime - MessageValidator.MessageValidatorBuilder.VALID_PAST_DELAY - 1;
    long futureTime = currentTime + DELTA_TIME;

    validator.validPastTimes(currentTime);
    assertThrows(IllegalArgumentException.class, () -> validator.validPastTimes(futureTime));
    assertThrows(IllegalArgumentException.class, () -> validator.validPastTimes(pastTime));
  }

  @Test
  public void testOrderedTimes() {
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
  public void testIsBase64() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    String validBase64 = Base64.getEncoder().encodeToString("test data".getBytes());
    String invalidBase64 = "This is not a valid Base64 string!";
    String field = "testField";

    validator.isBase64(validBase64, field);
    assertThrows(IllegalArgumentException.class, () -> validator.isBase64(invalidBase64, field));
    assertThrows(IllegalArgumentException.class, () -> validator.isBase64(null, field));
  }

  @Test
  public void testStringNotEmpty() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    String validString = "test string";
    String emptyString = "";
    String field = "testField";

    validator.stringNotEmpty(validString, field);
    assertThrows(
        IllegalArgumentException.class, () -> validator.stringNotEmpty(emptyString, field));
    assertThrows(IllegalArgumentException.class, () -> validator.stringNotEmpty(null, field));
  }

  @Test
  public void testListNotEmpty() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    List<Integer> valid1 = Arrays.asList(1, 2, 3);
    List<String> valid2 = Arrays.asList("a", "b");
    List<String> invalid = new ArrayList<>();

    validator.listNotEmpty(valid1);
    validator.listNotEmpty(valid2);
    assertThrows(IllegalArgumentException.class, () -> validator.listNotEmpty(invalid));
    assertThrows(IllegalArgumentException.class, () -> validator.listNotEmpty(null));
  }

  @Test
  public void testNoListDuplicates() {
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

  @Test
  public void testValidVotesWithPlainVotes() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();

    PlainVote plainVote1 = new PlainVote(QUESTION_ID1, 1, false, "something", ELECTION_ID);
    PlainVote plainVote2 = new PlainVote(QUESTION_ID2, 2, false, "something else", ELECTION_ID);
    List<Vote> validPlainVotes = Arrays.asList(plainVote1, plainVote2);

    validator.validVotes(validPlainVotes);

    PlainVote plainVote3 = new PlainVote("not base 64", 1, false, "something", ELECTION_ID);
    List<Vote> invalidPlainVotes = Arrays.asList(plainVote1, plainVote3);

    assertThrows(IllegalArgumentException.class, () -> validator.validVotes(invalidPlainVotes));
  }

  @Test
  public void testValidUrl() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();

    validator.validUrl("http://example.com");
    validator.validUrl("https://example.com");
    validator.validUrl("ws://example.com");
    validator.validUrl("wss://10.0.2.2:8000/path");
    validator.validUrl("https://example.com/path/to/file.html");
    validator.validUrl("wss://example.com/path/to/file");

    assertThrows(IllegalArgumentException.class, () -> validator.validUrl("Random String"));
    assertThrows(IllegalArgumentException.class, () -> validator.validUrl("example.com"));
    assertThrows(IllegalArgumentException.class, () -> validator.validUrl("http:example.com"));
    assertThrows(IllegalArgumentException.class, () -> validator.validUrl("://example.com"));
    assertThrows(IllegalArgumentException.class, () -> validator.validUrl("http://example."));
  }

  @Test
  public void testValidEmoji() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();
    String field = "testField";

    validator.isValidEmoji("\uD83D\uDC4D", field);
    validator.isValidEmoji("\uD83D\uDC4E", field);
    validator.isValidEmoji("❤️", field);

    assertThrows(
        IllegalArgumentException.class, () -> validator.isValidEmoji("\uD83D\uDE00", field));
    assertThrows(IllegalArgumentException.class, () -> validator.isValidEmoji("U+1F600", field));
    assertThrows(
        IllegalArgumentException.class, () -> validator.isValidEmoji("random string", field));
  }

  @Test
  public void testValidPopCHAUrl() {
    MessageValidator.MessageValidatorBuilder validator = MessageValidator.verify();

    String laoId = "6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=";

    String valid =
        "http://localhost:9100/authorize?response_mode=query&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ";
    String valid2 =
        "https://be1.personhood.online/authorize?response_mode=fragment&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile+random&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA";
    String missingClientId =
        "http://localhost:9100/authorize?response_mode=query&response_type=id_token&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ";
    String invalidResponseType =
        "http://localhost:9100/authorize?response_mode=query&response_type=token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ";
    String missingRequiredScope =
        "http://localhost:9100/authorize?response_mode=query&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+random&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ";
    String invalidResponseMode =
        "http://localhost:9100/authorize?response_mode=random&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=6IQ-Q3S4ISaxZ-hLDelTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ";
    String invalidLao =
        "http://localhost:9100/authorize?response_mode=random&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=6IQ-Q3S4ISaxZ-hLTylTszYxhJkNQ1gC4JjMxr4jy6Y=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ";

    validator.isValidPoPCHAUrl(valid, laoId);
    validator.isValidPoPCHAUrl(valid2, laoId);

    assertThrows(
        IllegalArgumentException.class, () -> validator.isValidPoPCHAUrl(missingClientId, laoId));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.isValidPoPCHAUrl(invalidResponseType, laoId));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.isValidPoPCHAUrl(missingRequiredScope, laoId));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.isValidPoPCHAUrl(invalidResponseMode, laoId));
    assertThrows(
        IllegalArgumentException.class, () -> validator.isValidPoPCHAUrl(invalidLao, laoId));
  }
}
