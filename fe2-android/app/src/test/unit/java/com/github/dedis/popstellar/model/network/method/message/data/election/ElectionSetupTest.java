package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonUtilsTest;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;
import com.google.gson.JsonParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ElectionSetupTest {

  private static final String ELECTION_NAME = "New election";
  private static final long CREATION = Instant.now().getEpochSecond();
  private static final long START = CREATION;
  private static final long END = START + 1;
  private static final PublicKey ORGANIZER = generatePublicKey();
  private static final ElectionQuestion.Question QUESTION1 =
      new Question(
          "Which is the best ?", "Plurality", Arrays.asList("Option a", "Option b"), false);
  private static final ElectionQuestion.Question QUESTION2 =
      new Question(
          "Who is the best ?", "Plurality", Arrays.asList("candidate1", "candidate2"), false);
  private static final ElectionQuestion.Question QUESTION3 =
      new Question(
          "Who is the best ?", "Plurality", Arrays.asList("candidate1", "candidate2"), false);
  private static final List<Question> QUESTIONS = Arrays.asList(QUESTION1, QUESTION2);
  private static final List<Question> QUESTIONS_DUPLICATES =
      Arrays.asList(QUESTION1, QUESTION2, QUESTION3);
  private final String LAO_ID = Lao.generateLaoId(ORGANIZER, CREATION, ELECTION_NAME);
  private final ElectionSetup openBallotSetup =
      new ElectionSetup(
          ELECTION_NAME, CREATION, START, END, LAO_ID, ElectionVersion.OPEN_BALLOT, QUESTIONS);
  private final ElectionSetup secretBallotSetup =
      new ElectionSetup(
          ELECTION_NAME, CREATION, START, END, LAO_ID, ElectionVersion.SECRET_BALLOT, QUESTIONS);

  @Test
  public void electionSetupGetterReturnsCorrectId() {
    // Hash('Election'||lao_id||created_at||name)
    String expectedId =
        Hash.hash(
            EventType.ELECTION.getSuffix(),
            openBallotSetup.getLaoId(),
            Long.toString(openBallotSetup.getCreation()),
            openBallotSetup.getName());
    assertThat(openBallotSetup.getElectionId(), is(expectedId));
  }

  @Test
  public void getNameTest() {
    assertThat(openBallotSetup.getName(), is(ELECTION_NAME));
  }

  @Test
  public void getEndTimeTest() {
    assertThat(openBallotSetup.getEndTime(), is(END));
  }

  @Test
  public void getLaoTest() {
    assertThat(openBallotSetup.getLaoId(), is(LAO_ID));
  }

  @Test
  public void electionSetupOnlyOneQuestion() {
    assertThat(openBallotSetup.getQuestions().size(), is(2));
  }

  @Test
  public void getObjectTest() {
    assertThat(openBallotSetup.getObject(), is(Objects.ELECTION.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(openBallotSetup.getAction(), is(Action.SETUP.getAction()));
  }

  @Test
  public void getVersionTest() {
    assertEquals(
        ElectionVersion.OPEN_BALLOT.getStringBallotVersion(),
        openBallotSetup.getElectionVersion().getStringBallotVersion());
    assertEquals(
        ElectionVersion.SECRET_BALLOT.getStringBallotVersion(),
        secretBallotSetup.getElectionVersion().getStringBallotVersion());
  }

  @Test
  public void constructorFailsWithLaoIdNotBase64() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                ELECTION_NAME,
                CREATION,
                START,
                END,
                "invalid id",
                ElectionVersion.OPEN_BALLOT,
                QUESTIONS_DUPLICATES));
  }

  @Test
  public void constructorFailsWithEmptyElectionName() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                "",
                CREATION,
                START,
                END,
                LAO_ID,
                ElectionVersion.OPEN_BALLOT,
                QUESTIONS_DUPLICATES));
  }

  @Test
  public void constructorFailsWithDuplicateQuestions() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                ELECTION_NAME,
                CREATION,
                START,
                END,
                LAO_ID,
                ElectionVersion.OPEN_BALLOT,
                QUESTIONS_DUPLICATES));
  }

  @Test
  public void constructorFailsWithUnorderedTimes() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                ELECTION_NAME,
                CREATION,
                START,
                START - 1,
                LAO_ID,
                ElectionVersion.OPEN_BALLOT,
                QUESTIONS));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                ELECTION_NAME,
                CREATION,
                CREATION - 1,
                END,
                LAO_ID,
                ElectionVersion.OPEN_BALLOT,
                QUESTIONS));
  }

  @Test
  public void constructorFailsWithNegativeTimes() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                ELECTION_NAME, CREATION, -1, END, LAO_ID, ElectionVersion.OPEN_BALLOT, QUESTIONS));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                ELECTION_NAME,
                CREATION,
                START,
                -1,
                LAO_ID,
                ElectionVersion.OPEN_BALLOT,
                QUESTIONS));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ElectionSetup(
                ELECTION_NAME,
                CREATION,
                START,
                -1,
                LAO_ID,
                ElectionVersion.OPEN_BALLOT,
                QUESTIONS));
  }

  @Test
  public void electionSetupEqualsTest() {
    ElectionSetup openBallotSetup2 =
        new ElectionSetup(
            ELECTION_NAME, CREATION, START, END, LAO_ID, ElectionVersion.OPEN_BALLOT, QUESTIONS);

    //noinspection EqualsWithItself
    assertThat(openBallotSetup.equals(openBallotSetup), is(true));
    assertThat(openBallotSetup.equals(openBallotSetup2), is(true));
    assertNotEquals(openBallotSetup, secretBallotSetup);
  }

  @Test
  public void jsonValidationTest() {

    // Check that valid data is successfully parses
    JsonUtilsTest.testData(openBallotSetup);
    JsonUtilsTest.testData(secretBallotSetup);

    String pathDir = "protocol/examples/messageData/election_setup/";

    String jsonValid1 = JsonUtilsTest.loadFile(pathDir + "election_setup.json");
    String jsonValid2 = JsonUtilsTest.loadFile(pathDir + "election_setup_secret_ballot.json");

    JsonUtilsTest.parse(jsonValid1);
    JsonUtilsTest.parse(jsonValid2);

    // Check that invalid data is rejected
    String jsonInvalid1 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_created_at_negative.json");
    String jsonInvalid2 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_end_time_before_created_at.json");
    String jsonInvalid3 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_end_time_negative.json");
    String jsonInvalid4 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_id_invalid_hash.json");
    String jsonInvalid5 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_lao_id_not_base64.json");
    String jsonInvalid6 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_lao_id_invalid_hash.json");
    String jsonInvalid7 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_lao_id_not_base64.json");
    String jsonInvalid8 = JsonUtilsTest.loadFile(pathDir + "bad_election_setup_missing_name.json");
    String jsonInvalid9 = JsonUtilsTest.loadFile(pathDir + "bad_election_setup_name_empty.json");
    String jsonInvalid10 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_question_empty.json");
    String jsonInvalid11 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_question_id_invalid_hash.json");
    String jsonInvalid12 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_question_id_not_base64.json");
    String jsonInvalid13 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_question_voting_method_invalid.json");
    String jsonInvalid14 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_start_time_before_created_at.json");
    String jsonInvalid15 =
        JsonUtilsTest.loadFile(pathDir + "bad_election_setup_start_time_negative.json");

    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid2));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid3));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid4));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid5));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid6));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid7));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid8));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid9));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid10));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid11));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid12));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid13));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid14));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid15));
  }

  @Test
  public void toStringTest() {
    String setupElectionStringTest =
        String.format(
            "ElectionSetup={"
                + "version='%s', "
                + "id='%s', "
                + "lao='%s', "
                + "name='%s', "
                + "createdAt=%d, "
                + "startTime=%d, "
                + "endTime=%d, "
                + "questions=%s}",
            ElectionVersion.OPEN_BALLOT,
            Election.generateElectionSetupId(LAO_ID, CREATION, ELECTION_NAME),
            LAO_ID,
            ELECTION_NAME,
            CREATION,
            START,
            END,
            Arrays.toString(openBallotSetup.getQuestions().toArray()));
    assertEquals(setupElectionStringTest, openBallotSetup.toString());
  }
}
