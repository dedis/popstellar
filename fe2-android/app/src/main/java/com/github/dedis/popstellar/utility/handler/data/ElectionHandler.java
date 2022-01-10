package com.github.dedis.popstellar.utility.handler.data;

import static com.github.dedis.popstellar.model.objects.event.EventState.CLOSED;
import static com.github.dedis.popstellar.model.objects.event.EventState.OPENED;
import static com.github.dedis.popstellar.model.objects.event.EventState.RESULTS_READY;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;

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
   * Process an ElectionSetup message.
   *
   * @param context the HandlerContext of the message
   * @param electionSetup the message that was received
   */
  public static void handleElectionSetup(HandlerContext context, ElectionSetup electionSetup) {
    LAORepository laoRepository = context.getLaoRepository();
    String channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    if (laoRepository.isLaoChannel(channel)) {
      Lao lao = laoRepository.getLaoByChannel(channel);
      Log.d(TAG, "handleElectionSetup: channel " + channel + " name " + electionSetup.getName());

      Election election =
          new Election(lao.getId(), electionSetup.getCreation(), electionSetup.getName());
      election.setChannel(channel + "/" + election.getId());
      election.setElectionQuestions(electionSetup.getQuestions());

      election.setStart(electionSetup.getStartTime());
      election.setEnd(electionSetup.getEndTime());
      election.setEventState(OPENED);

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
   * @param context the HandlerContext of the message
   * @param electionResult the message that was received
   */
  public static void handleElectionResult(HandlerContext context, ElectionResult electionResult)
      throws DataHandlingException {
    LAORepository laoRepository = context.getLaoRepository();
    String channel = context.getChannel();

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
   * @param context the HandlerContext of the message
   * @param electionEnd the message that was received
   */
  @SuppressWarnings("unused")
  public static void handleElectionEnd(HandlerContext context, ElectionEnd electionEnd) {
    LAORepository laoRepository = context.getLaoRepository();
    String channel = context.getChannel();

    Log.d(TAG, "handleElectionEnd: channel " + channel);
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);
    election.setEventState(CLOSED);
    lao.updateElection(election.getId(), election);
  }

  /**
   * Process a CastVote message.
   *
   * @param context the HandlerContext of the message
   * @param castVote the message that was received
   */
  public static void handleCastVote(HandlerContext context, CastVote castVote) {
    LAORepository laoRepository = context.getLaoRepository();
    String channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Log.d(TAG, "handleCastVote: channel " + channel);
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);

    // Verify the vote was created before the end of the election or the election is not closed yet
    if (election.getEndTimestamp() >= castVote.getCreation() || election.getState() != CLOSED) {
      // Retrieve previous cast vote message stored for the given sender
      Optional<MessageID> previousMessageIdOption =
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
    }
  }

  public static WitnessMessage electionSetupWitnessMessage(MessageID messageId, Election election) {
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
