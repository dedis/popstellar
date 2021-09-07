package com.github.dedis.popstellar.model.data.handler;

import static com.github.dedis.popstellar.model.event.EventState.CLOSED;
import static com.github.dedis.popstellar.model.event.EventState.OPENED;
import static com.github.dedis.popstellar.model.event.EventState.RESULTS_READY;

import android.util.Log;
import com.github.dedis.popstellar.model.Election;
import com.github.dedis.popstellar.model.Lao;
import com.github.dedis.popstellar.model.WitnessMessage;
import com.github.dedis.popstellar.model.data.LAORepository;
import com.github.dedis.popstellar.model.network.method.message.data.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Election messages handler class
 */
public class ElectionHandler {

  public static final String TAG = ElectionHandler.class.getSimpleName();

  /**
   * Process a ElectionSetup message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel       the channel on which the message was received
   * @param electionSetup the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleElectionSetup(LAORepository laoRepository, String channel,
      ElectionSetup electionSetup,
      String messageId) {
    //election setup msg should be sent on an LAO channel
    if (laoRepository.isLaoChannel(channel)) {
      Lao lao = laoRepository.getLaoByChannel(channel);
      Log.d(TAG, "handleElectionSetup: " + channel + " name " + electionSetup.getName());

      Election election = new Election();
      election.setId(electionSetup.getId());
      election.setName(electionSetup.getName());
      election.setCreation(electionSetup.getCreation());
      election.setChannel(channel + "/" + election.getId());
      election.setElectionQuestions(electionSetup.getQuestions());

      election.setStart(electionSetup.getStartTime());
      election.setEnd(electionSetup.getEndTime());
      election.setEventState(OPENED);

      //Once the election is created, we subscribe to the election channel
      // TODO: subscribe somewhere else.
      laoRepository.sendSubscribe(election.getChannel());
      Log.d(TAG, "election id being put is " + election.getId());
      lao.updateElection(election.getId(), election);

      WitnessMessage message = new WitnessMessage(messageId);
      message.setTitle("New Election Setup ");
      // TODO : In the future display for multiple questions
      message.setDescription(
          "Name : " + election.getName() + "\n" + "Election ID : " + election.getId() + "\n"
              + "Question : " + election.getElectionQuestions().get(0).getQuestion() + "\n"
              + "Message ID : " + messageId);

      lao.updateWitnessMessage(messageId, message);
    }
    return false;
  }

  /**
   * Process a ElectionResult message.
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
      throw new IllegalArgumentException("the questions results shouldn't be empty");
    }
    Log.d(TAG, "size of resultsQuestions is " + resultsQuestions.size());
    election.setResults(resultsQuestions);
    election.setEventState(RESULTS_READY);
    lao.updateElection(election.getId(), election);
    return false;
  }

  /**
   * Process a ElectionEnd message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel       the channel on which the message was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleElectionEnd(LAORepository laoRepository, String channel) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);
    election.setEventState(CLOSED);
    lao.updateElection(election.getId(), election);
    return false;
  }

  /**
   * Process a CreateRollCall message.
   *
   * @param laoRepository the repository to access the messages, election and LAO of the channel
   * @param channel       the channel on which the message was received
   * @param data          TODO: change params to only take message, channel and laoRep.
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleCastVote(LAORepository laoRepository, String channel, CastVote data,
      String senderPk, String messageId) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Election election = laoRepository.getElectionByChannel(channel);

    //We ignore the vote iff the election is ended and the cast vote message was created after the end timestamp
    if (election.getEndTimestamp() >= data.getCreation() || election.getState() != CLOSED) {
      /* We retrieve previous cast vote message stored for the given sender, and consider the new vote iff its creation
      is after (hence preventing reordering attacks) */
      Optional<String> previousMessageIdOption = election.getMessageMap().entrySet().stream()
          .filter(entry -> senderPk.equals(entry.getValue())).map(Map.Entry::getKey).findFirst();
      //If there is no previous message, or that this message is the last of all received messages, then we consider the votes
      if (!previousMessageIdOption.isPresent() ||
          ((CastVote) laoRepository.getMessageById().get(previousMessageIdOption.get()).getData())
              .getCreation()
              <= data.getCreation()) {
        election.putVotesBySender(senderPk, data.getVotes());
        election.putSenderByMessageId(senderPk, messageId);
        lao.updateElection(election.getId(), election);
      }
    }
    return false;
  }
}
