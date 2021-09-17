package com.github.dedis.popstellar.model.network;

import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;
import java.util.List;

/**
 * ID Generator class
 */
public class IdGenerator {

  static final String SUFFIX_ELECTION_QUESTION = "Question";
  static final String SUFFIX_ELECTION_VOTE = "Vote";

  private IdGenerator() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Generate the id for dataCreateLao and dataUpdateLao. https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataCreateLao.json
   * https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataUpdateLao.json
   *
   * @param organizer ID of the organizer
   * @param creation  creation time of the LAO
   * @param name      original or updated name of the LAO
   * @return the ID of CreateLao or UpdateLao computed as Hash(organizer||creation||name)
   */
  public static String generateLaoId(String organizer, long creation, String name) {
    return Hash.hash(organizer, Long.toString(creation), name);
  }

  /**
   * Generate the id for dataCreateMeeting. https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataCreateMeeting.json
   *
   * @param laoId    ID of the LAO
   * @param creation creation time of the Meeting
   * @param name     original or updated name of the Meeting
   * @return the ID of CreateMeeting computed as Hash('M'||lao_id||creation||name)
   */
  public static String generateCreateMeetingId(String laoId, long creation, String name) {
    return Hash.hash(EventType.MEETING.getSuffix(), laoId, Long.toString(creation), name);
  }

  /**
   * Generate the id for dataCreateRollCall. https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataCreateRollCall.json
   *
   * @param laoId    ID of the LAO
   * @param creation creation time of RollCall
   * @param name     name of RollCall
   * @return the ID of CreateRollCall computed as Hash('R'||lao_id||creation||name)
   */
  public static String generateCreateRollCallId(String laoId, long creation, String name) {
    return Hash.hash(EventType.ROLL_CALL.getSuffix(), laoId, Long.toString(creation), name);
  }

  /**
   * Generate the id for dataOpenRollCall. https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataOpenRollCall.json
   *
   * @param laoId    ID of the LAO
   * @param opens    id of RollCall to open
   * @param openedAt open time of RollCall
   * @return the ID of OpenRollCall computed as Hash('R'||lao_id||opens||opened_at)
   */
  public static String generateOpenRollCallId(String laoId, String opens, long openedAt) {
    return Hash.hash(EventType.ROLL_CALL.getSuffix(), laoId, opens, Long.toString(openedAt));
  }

  /**
   * Generate the id for dataCloseRollCall. https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataCloseRollCall.json
   *
   * @param laoId    ID of the LAO
   * @param closes   id of RollCall to close
   * @param closedAt closing time of RollCall
   * @return the ID of CloseRollCall computed as Hash('R'||lao_id||closes||closed_at)
   */
  public static String generateCloseRollCallId(String laoId, String closes, long closedAt) {
    return Hash.hash(EventType.ROLL_CALL.getSuffix(), laoId, closes, Long.toString(closedAt));
  }

  /**
   * Generate the id for dataElectionSetup. https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataElectionSetup.json
   *
   * @param laoId     ID of the LAO
   * @param createdAt creation time of the election
   * @param name      name of the election
   * @return the ID of ElectionSetup computed as Hash('Election'||lao_id||created_at||name)
   */
  public static String generateElectionSetupId(String laoId, long createdAt, String name) {
    return Hash.hash(EventType.ELECTION.getSuffix(), laoId, Long.toString(createdAt), name);
  }

  /**
   * Generate the id for a question of dataElectionSetup and dataElectionResult.
   * https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataElectionSetup.json
   * https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataElectionResult.json
   *
   * @param electionId ID of the Election
   * @param question   question of the Election
   * @return the ID of an election question computed as Hash(“Question”||election_id||question)
   */
  public static String generateElectionQuestionId(String electionId, String question) {
    return Hash.hash(SUFFIX_ELECTION_QUESTION, electionId, question);
  }

  /**
   * Generate the id for a vote of dataCastVote. https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataCastVote.json
   *
   * @param electionId     ID of the Election
   * @param questionId     ID of the Election question
   * @param voteIndex      index(es) of the vote
   * @param writeIn        string representing the write in
   * @param writeInEnabled boolean representing if write enabled or not
   * @return the ID of an election question computed as Hash('Vote'||election_id||question_id||(vote_index(es)|write_in))
   */
  public static String generateElectionVoteId(String electionId, String questionId,
      List<Integer> voteIndex, String writeIn, boolean writeInEnabled) {
    // If write_in is enabled the id is formed with the write_in string
    // If write_in is not enabled the id is formed with the vote indexes (formatted as [int1, int2, ...])
    return Hash.hash(SUFFIX_ELECTION_VOTE, electionId, questionId,
        writeInEnabled ? writeIn : voteIndex.toString());
  }
}
