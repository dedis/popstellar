package be.model;

import be.utils.Hash;
import be.utils.RandomUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
   * Copies the existing election but switches the creation and start time of the election.
   * Recomputes the election id to match the new creation time.
   *
   * @return copy of the election with switched creation and start time
   */
  public Election switchCreationAndStart(){
    String newId = generateElectionSetupId(getLaoId(channel), start, name);
    return new Election(channel, newId, name, version, start, creation, end, questions);
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
   * Generate the id for a vote of dataCastVote.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCastVote.json
   *
   * @param electionId ID of the Election
   * @param questionId ID of the Election question
   * @param voteIndex index(es) of the vote
   * @param writeIn string representing the write in
   * @param writeInEnabled boolean representing if write enabled or not
   * @return the ID of an election question computed as
   *     Hash('Vote'||election_id||question_id||(vote_index(es)|write_in))
   */
  public static String generateElectionVoteId(
    String electionId,
    String questionId,
    Integer voteIndex,
    String writeIn,
    boolean writeInEnabled) {
    // If write_in is enabled the id is formed with the write_in string
    // If write_in is not enabled the id is formed with the vote indexes (formatted as int1, int2,
    // ). The vote are concatenated and brackets are removed from the array toString representation
    return Hash.hash(
      "Vote", electionId, questionId, writeInEnabled ? writeIn : voteIndex.toString());
  }

  /**
   * Creates a random valid election question with 2 ballot options, plurality voting method and write in set to false.
   * Adds the question to the current election.
   *
   * @return a valid election question
   */
  public ElectionQuestion createQuestion(){
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

  /**
   *
   * @param plainVotes
   * @return
   */
  public CastVote castVote(PlainVote... plainVotes){
    long createdAt = Instant.now().getEpochSecond();
    return new CastVote(createdAt, plainVotes);
  }

  /** Contains the data to create a valid open election message */
  public static class ElectionOpen{
    public long openedAt;

    public ElectionOpen(long openedAt){
      this.openedAt = openedAt;
    }
  }

  /** Contains the data to create a valid open election message */
  public static class CastVote{
    public long createdAt;
    public List<PlainVote> votes;

    public CastVote(long createdAt, PlainVote[] votes){
      this.createdAt = createdAt;
      this.votes = List.of(votes);
    }
  }

  private String getLaoId(String electionChannel){
    int index = electionChannel.indexOf('/');
    if (index != -1) {
      return electionChannel.substring(0, index);
    } else {
      throw new IllegalArgumentException(electionChannel + " is not a valid election channel");
    }
  }
}
