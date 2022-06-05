package com.github.dedis.popstellar.model.objects;

import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Election extends Event {

  private Channel channel;
  private String id;
  private String name;
  private long creation;
  private long start;
  private long end;
  private List<ElectionQuestion> electionQuestions;

  // Map that associates each sender pk to their votes
  private final Map<PublicKey, List<ElectionVote>> voteMap;
  // Map that associates each messageId to its sender
  private final Map<MessageID, PublicKey> messageMap;

  private final MutableLiveData<EventState> state = new MutableLiveData<>();

  // Results of an election (associated to a question id)
  private final Map<String, List<QuestionResult>> results;

  public Election(String laoId, long creation, String name) {
    this.id = Election.generateElectionSetupId(laoId, creation, name);
    this.name = name;
    this.creation = creation;
    this.results = new HashMap<>();
    this.electionQuestions = new ArrayList<>();
    this.voteMap = new HashMap<>();
    this.messageMap = new TreeMap<>(Comparator.comparing(MessageID::getEncoded));
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    if (id == null) {
      throw new IllegalArgumentException("election id shouldn't be null");
    }
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("election name shouldn't be null");
    }
    this.name = name;
  }

  public long getCreation() {
    return creation;
  }

  public long getCreationInMillis() {
    return getCreation() * 1000;
  }

  public Channel getChannel() {
    return channel;
  }

  public List<ElectionQuestion> getElectionQuestions() {
    return electionQuestions;
  }

  public void setCreation(long creation) {
    if (creation < 0) {
      throw new IllegalArgumentException();
    }
    this.creation = creation;
  }

  public void setEventState(EventState state) {
    this.state.postValue(state);
  }

  public MutableLiveData<EventState> getState() {
    return state;
  }

  public void setStart(long start) {
    if (start < 0) {
      throw new IllegalArgumentException();
    }
    this.start = start;
  }

  public void setEnd(long end) {
    if (end < 0) {
      throw new IllegalArgumentException();
    }
    this.end = end;
  }

  public Map<MessageID, PublicKey> getMessageMap() {
    return messageMap;
  }

  public void putVotesBySender(PublicKey senderPk, List<ElectionVote> votes) {
    if (senderPk == null) {
      throw new IllegalArgumentException("Sender public key cannot be null.");
    }
    if (votes == null || votes.isEmpty()) {
      throw new IllegalArgumentException("votes cannot be null or empty");
    }
    // The list must be sorted by order of question ids
    List<ElectionVote> votesCopy = new ArrayList<>(votes);
    votesCopy.sort(Comparator.comparing(ElectionVote::getQuestionId));
    voteMap.put(senderPk, votesCopy);
  }

  public void putSenderByMessageId(PublicKey senderPk, MessageID messageId) {
    if (senderPk == null || messageId == null) {
      throw new IllegalArgumentException("Sender public key or message id cannot be null.");
    }
    messageMap.put(messageId, senderPk);
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public void setElectionQuestions(List<ElectionQuestion> electionQuestions) {
    if (electionQuestions == null) {
      throw new IllegalArgumentException();
    }
    this.electionQuestions = electionQuestions;
  }

  @Override
  public long getStartTimestamp() {
    return start;
  }

  @Override
  public long getEndTimestamp() {
    return end;
  }

  /**
   * Computes the hash for the registered votes, when terminating an election (sorted by message
   * id's alphabetical order)
   *
   * @return the hash of all registered votes
   */
  public String computerRegisteredVotes() {
    List<String> listOfVoteIds = new ArrayList<>();
    // Since messageMap is a TreeMap, votes will already be sorted in the alphabetical order of
    // messageIds
    for (PublicKey senderPk : messageMap.values()) {
      for (ElectionVote vote : voteMap.get(senderPk)) {
        listOfVoteIds.add(vote.getId());
      }
    }
    if (listOfVoteIds.isEmpty()) {
      return "";
    } else {
      return Hash.hash(listOfVoteIds.toArray(new String[0]));
    }
  }

  public void setResults(List<ElectionResultQuestion> electionResultsQuestions) {
    if (electionResultsQuestions == null) {
      throw new IllegalArgumentException("the list of winners should not be null");
    }
    for (ElectionResultQuestion resultQuestion : electionResultsQuestions) {
      List<QuestionResult> questionResults = resultQuestion.getResult();
      String questionId = resultQuestion.getId();
      if (questionResults == null) {
        results.put(questionId, new ArrayList<>());
      } else {
        questionResults.sort((r1, r2) -> r2.getCount().compareTo(r1.getCount()));
        this.results.put(questionId, questionResults);
      }
    }
  }

  public List<QuestionResult> getResultsForQuestionId(String id) {
    return results.get(id);
  }

  @Override
  public EventType getType() {
    return EventType.ELECTION;
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
    return Hash.hash(EventType.ELECTION.getSuffix(), laoId, Long.toString(createdAt), name);
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
      List<Integer> voteIndex,
      String writeIn,
      boolean writeInEnabled) {
    // If write_in is enabled the id is formed with the write_in string
    // If write_in is not enabled the id is formed with the vote indexes (formatted as int1, int2,
    // ). The vote are concatenated and brackets are removed from the array toString representation
    String voteIndexFormat = voteIndex
            .toString()
            .replace("]", "")
            .replace("[", "");
    return Hash.hash(
            "Vote", electionId, questionId, writeInEnabled ? writeIn : voteIndexFormat);
  }

  @Override
  public String toString() {
    return "Election{"
        + "channel='"
        + channel
        + '\''
        + ", id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", creation="
        + creation
        + ", start="
        + start
        + ", end="
        + end
        + ", electionQuestions="
        + Arrays.toString(electionQuestions.toArray())
        + ", voteMap="
        + voteMap
        + ", messageMap="
        + messageMap
        + ", state="
        + state
        + ", results="
        + results
        + '}';
  }
}
