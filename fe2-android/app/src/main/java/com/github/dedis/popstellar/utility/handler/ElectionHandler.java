package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.model.objects.event.EventState.CLOSED;
import static com.github.dedis.popstellar.model.objects.event.EventState.OPENED;
import static com.github.dedis.popstellar.model.objects.event.EventState.RESULTS_READY;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.repository.LAORepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Election messages handler class
 */
public class ElectionHandler {

  public static final String TAG = ElectionHandler.class.getSimpleName();

  private ElectionHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Process an Election message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel       the channel on which the message was received
   * @param data          the data of the message that was received
   * @param messageId     the ID of the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleElectionMessage(LAORepository laoRepository, String channel,
      Data data,
      String messageId, String senderPk) {
    Log.d(TAG, "handle Election message");

    switch (Objects.requireNonNull(Action.find(data.getAction()))) {
      case SETUP:
        return handleElectionSetup(laoRepository, channel, (ElectionSetup) data, messageId);
      case RESULT:
        return handleElectionResult(laoRepository, channel, (ElectionResult) data);
      case END:
        return handleElectionEnd(laoRepository, channel);
      case CAST_VOTE:
        return handleCastVote(laoRepository, channel, (CastVote) data, senderPk, messageId);
      default:
        return true;
    }
  }

  /**
   * Process an ElectionSetup message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel       the channel on which the message was received
   * @param electionSetup the message that was received
   * @param messageId     the ID of message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleElectionSetup(LAORepository laoRepository, String channel,
      ElectionSetup electionSetup,
      String messageId) {
    if (laoRepository.isLaoChannel(channel)) {
      Lao lao = laoRepository.getLaoByChannel(channel);
      Log.d(TAG, "handleElectionSetup: channel " + channel + " name " + electionSetup.getName());

      Election election = new Election(lao.getId(), electionSetup.getCreation(),
          electionSetup.getName());
      election.setChannel(channel + "/" + election.getId());
      election.setElectionQuestions(electionSetup.getQuestions());

      election.setStart(electionSetup.getStartTime());
      election.setEnd(electionSetup.getEndTime());
      election.setEventState(OPENED);

      //Once the election is created, we subscribe to the election channel
      laoRepository.sendSubscribe(election.getChannel());
      Log.d(TAG, "election id " + election.getId());
      lao.updateElection(election.getId(), election);

      lao.updateWitnessMessage(messageId, electionSetupWitnessMessage(messageId, election));
    }
    return false;
  }

  /**
   * Process an ElectionResult message.
   *
   * @param laoRepository  the repository to access the election and LAO of the channel
   * @param channel        the channel on which the message was received
   * @param electionResult the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleElectionResult(LAORepository laoRepository, String channel,
      ElectionResult electionResult) {
    Log.d(TAG, "handling election result");
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);

    List<ElectionResultQuestion> resultsQuestions = electionResult.getElectionQuestionResults();
    if (resultsQuestions.isEmpty()) {
      throw new IllegalArgumentException("the questions results is empty");
    }
    Log.d(TAG, "size of resultsQuestions is " + resultsQuestions.size());
    election.setResults(resultsQuestions);
    election.setEventState(RESULTS_READY);
    lao.updateElection(election.getId(), election);
    return false;
  }

  /**
   * Process an ElectionEnd message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel       the channel on which the message was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleElectionEnd(LAORepository laoRepository, String channel) {
    Log.d(TAG, "handleElectionEnd: channel " + channel);
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);
    election.setEventState(CLOSED);
    lao.updateElection(election.getId(), election);
    return false;
  }

  /**
   * Process a CastVote message.
   *
   * @param laoRepository the repository to access the messages, election and LAO of the channel
   * @param channel       the channel on which the message was received
   * @param castVote      the message that was received
   * @param senderPk      the public key of the sender
   * @param messageId     the ID of the message
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleCastVote(LAORepository laoRepository, String channel,
      CastVote castVote, String senderPk, String messageId) {
    Log.d(TAG, "handleCastVote: channel " + channel);
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);

    // Verify the vote was created before the end of the election or the election is not closed yet
    if (election.getEndTimestamp() >= castVote.getCreation() || election.getState() != CLOSED) {
      // Retrieve previous cast vote message stored for the given sender
      Optional<String> previousMessageIdOption = election.getMessageMap().entrySet().stream()
          .filter(entry -> senderPk.equals(entry.getValue())).map(Map.Entry::getKey).findFirst();
      // Retrieve the creation time of the previous cast vote, if doesn't exist replace with min value
      long previousMessageCreation = previousMessageIdOption
          .map(s -> ((CastVote) laoRepository.getMessageById().get(s).getData()).getCreation())
          .orElse(Long.MIN_VALUE);

      // Verify the current cast vote message is the last one received
      if (previousMessageCreation <= castVote.getCreation()) {
        election.putVotesBySender(senderPk, castVote.getVotes());
        election.putSenderByMessageId(senderPk, messageId);
        lao.updateElection(election.getId(), election);
      }
    }
    return false;
  }

  public static WitnessMessage electionSetupWitnessMessage(String messageId, Election election) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("New Election Setup");
    message.setDescription(
        "Name : " + election.getName() + "\n" +
            "Election ID : " + election.getId() + "\n" +
            "Question : " + election.getElectionQuestions().get(0).getQuestion() + "\n" +
            "Message ID : " + messageId);
    return message;
  }
}
