package be.model;

import be.utils.Hash;
import be.utils.RandomUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Election {

  public String channel;
  public String id;
  public String name;
  public String version;
  public long creation;
  public long start;
  public long end;
  public List<ElectionQuestion> questions;

  private final static String ELECTION_SUFFIX = "Election";

  public Election(String channel, String id, String name, String version, long creation, long start, long end, List<ElectionQuestion> questions) {
    this.channel = channel;
    this.id = id;
    this.name = name;
    this.version = version;
    this.creation = creation;
    this.start = start;
    this.end = end;
    this.questions = questions;
  }

  /**
   * Generate the id for dataElectionSetup.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataElectionSetup.json
   *
   * @param laoId ID of the LAO
   * @param createdAt creation time of the election
   * @param name name of the election
   * @return the ID of ElectionSetup computed as Hash('Election'||lao_id||created_at||name)
   */
  public static String generateElectionSetupId(String laoId, long createdAt, String name) {
    return Hash.hash(ELECTION_SUFFIX, laoId, Long.toString(createdAt), name);
  }

  /**
   * Generate the id for a question of dataElectionSetup and dataElectionResult.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataElectionSetup.json
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataElectionResult.json
   *
   * @param electionId ID of the Election
   * @param question question of the Election
   * @return the ID of an election question computed as Hash(“Question”||election_id||question)
   */
  public static String generateElectionQuestionId(String electionId, String question) {
    return Hash.hash("Question", electionId, question);
  }

  /**
   *
   * @return
   */
  public ElectionQuestion addRandomQuestion(){
    List<String> ballotOptions = new ArrayList<>();
    ballotOptions.add(RandomUtils.generateRandomName());
    ballotOptions.add(RandomUtils.generateRandomName());

    ElectionQuestion question =  new ElectionQuestion(
      id,
      RandomUtils.generateRandomName(),
      "Plurality",
      ballotOptions,
      false);

    questions.add(question);
    return question;
  }

  /**
   * @return an object containing the data to create a valid open election message
   */
  public ElectionOpen open(){
    long openedAt = Instant.now().getEpochSecond();
    return new ElectionOpen(openedAt);
  }

  /** Contains the data to create a valid open election message */
  public static class ElectionOpen{
    public long openedAt;

    public ElectionOpen(long openedAt){
      this.openedAt = openedAt;
    }
  }
}
