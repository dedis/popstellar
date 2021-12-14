package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.model.objects.event.EventState.CLOSED;
import static com.github.dedis.popstellar.model.objects.event.EventState.CREATED;
import static com.github.dedis.popstellar.model.objects.event.EventState.OPENED;
import static com.github.dedis.popstellar.model.objects.event.EventState.RESULTS_READY;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.election.OpenElection;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.UnhandledDataTypeException;
import com.github.dedis.popstellar.utility.error.UnknownDataActionException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Election messages handler class */
public final class ElectionHandler {

  public static final String TAG = ElectionHandler.class.getSimpleName();

  private ElectionHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Process an Election message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   * @param data the data of the message that was received
   * @param messageId the ID of the message that was received
   */
  public static void handleElectionMessage(
      LAORepository laoRepository, String channel, Data data, String messageId, String senderPk)
      throws DataHandlingException {
    Log.d(TAG, "handle Election message");

    Action action = Action.find(data.getAction());
    if (action == null) throw new UnknownDataActionException(data);

    switch (action) {
      case SETUP:
        handleElectionSetup(laoRepository, channel, (ElectionSetup) data, messageId);
        break;
      case RESULT:
        handleElectionResult(laoRepository, channel, (ElectionResult) data);
        break;
      case END:
        handleElectionEnd(laoRepository, channel);
        break;
      case CAST_VOTE:
        handleCastVote(laoRepository, channel, (CastVote) data, senderPk, messageId);
        break;
      case OPEN:
        handleOpenElection(laoRepository, channel, (OpenElection) data);
        break;
      default:
        Log.w(TAG, "Invalid action for a consensus object : " + data.getAction());
        throw new UnhandledDataTypeException(data, action.getAction());
    }
  }

  /**
   * Process an ElectionSetup message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   * @param electionSetup the message that was received
   * @param messageId the ID of message that was received
   */
  public static void handleElectionSetup(
      LAORepository laoRepository, String channel, ElectionSetup electionSetup, String messageId) {
    if (laoRepository.isLaoChannel(channel)) {
      Lao lao = laoRepository.getLaoByChannel(channel);
      Log.d(TAG, "handleElectionSetup: channel " + channel + " name " + electionSetup.getName());

      Election election =
          new Election(lao.getId(), electionSetup.getCreation(), electionSetup.getName());
      election.setChannel(channel + "/" + election.getId());
      election.setElectionQuestions(electionSetup.getQuestions());

      election.setStart(electionSetup.getStartTime());
      election.setEnd(electionSetup.getEndTime());
      election.setEventState(CREATED);

      // Once the election is created, we subscribe to the election channel
      laoRepository.sendSubscribe(election.getChannel());
      Log.d(TAG, "election id " + election.getId());
      lao.updateElection(election.getId(), election);

      lao.updateWitnessMessage(messageId, electionSetupWitnessMessage(messageId, election));
    }
  }

  /**
   * Process an ElectionResult message.
   *
   * @param laoRepository the repository to access the election and LAO of the channel
   * @param channel the channel on which the message was received
   * @param electionResult the message that was received
   */
  public static void handleElectionResult(
      LAORepository laoRepository, String channel, ElectionResult electionResult)
      throws DataHandlingException {
    Log.d(TAG, "handling election result");
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);

    List<ElectionResultQuestion> resultsQuestions = electionResult.getElectionQuestionResults();
    Log.d(TAG, "size of resultsQuestions is " + resultsQuestions.size());
    if (resultsQuestions.isEmpty())
      throw new DataHandlingException(electionResult, "the questions results is empty");

    election.setResults(resultsQuestions);
    election.setEventState(RESULTS_READY);
    lao.updateElection(election.getId(), election);
  }

  /**
   * Process an ElectionEnd message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   */
  public static void handleElectionEnd(LAORepository laoRepository, String channel) {
    Log.d(TAG, "handleElectionEnd: channel " + channel);
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);
    election.setEventState(CLOSED);
    lao.updateElection(election.getId(), election);
  }

  /**
   * Process a CastVote message.
   *
   * @param laoRepository the repository to access the messages, election and LAO of the channel
   * @param channel the channel on which the message was received
   * @param castVote the message that was received
   * @param senderPk the public key of the sender
   * @param messageId the ID of the message
   */
  public static void handleCastVote(
      LAORepository laoRepository,
      String channel,
      CastVote castVote,
      String senderPk,
      String messageId) {
    Log.d(TAG, "handleCastVote: channel " + channel);
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);

    if (election.getState() == OPENED) {
      // Retrieve previous cast vote message stored for the given sender
      Optional<String> previousMessageIdOption =
          election.getMessageMap().entrySet().stream()
              .filter(entry -> senderPk.equals(entry.getValue()))
              .map(Map.Entry::getKey)
              .findFirst();
      // Retrieve the creation time of the previous cast vote, if doesn't exist replace with min
      // value
      long previousMessageCreation =
          previousMessageIdOption
              .map(s -> laoRepository.getMessageById().get(s))
              .map(MessageGeneral::getData)
              .map(CastVote.class::cast)
              .map(CastVote::getCreation)
              .orElse(Long.MIN_VALUE);

      // Verify the current cast vote message is the last one received
      if (previousMessageCreation <= castVote.getCreation()) {
        election.putVotesBySender(senderPk, castVote.getVotes());
        election.putSenderByMessageId(senderPk, messageId);
        lao.updateElection(election.getId(), election);
      }
    } else {
      Log.w(TAG, "Received a CastVote but the election state was : " + election.getState());
    }
  }

  /**
   * Process an OpenElection message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   * @param openElection the message that was received
   */
  public static void handleOpenElection(
      LAORepository laoRepository, String channel, OpenElection openElection)
      throws DataHandlingException {
    Log.d(TAG, "handleOpenElection: channel " + channel);
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);

    if (election.getState() != CREATED) {
      throw new DataHandlingException(
          openElection,
          "received an OpenElection but the election state was : " + election.getState());
    }

    election.setEventState(OPENED);
    election.setStart(openElection.getOpenedAt());
    lao.updateElection(election.getId(), election);
  }

  public static WitnessMessage electionSetupWitnessMessage(String messageId, Election election) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("New Election Setup");
    message.setDescription(
        "Name : "
            + election.getName()
            + "\n"
            + "Election ID : "
            + election.getId()
            + "\n"
            + "Question : "
            + election.getElectionQuestions().get(0).getQuestion()
            + "\n"
            + "Message ID : "
            + messageId);
    return message;
  }
}
