package com.github.dedis.popstellar.utility.handler.data;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.utility.error.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import javax.inject.Inject;

import static com.github.dedis.popstellar.model.objects.event.EventState.*;

/** Election messages handler class */
public final class ElectionHandler {

  private static final Logger logger = LogManager.getLogger(ElectionHandler.class);

  private final LAORepository laoRepo;
  private final MessageRepository messageRepo;
  private final ElectionRepository electionRepository;

  @Inject
  public ElectionHandler(
      MessageRepository messageRepo, LAORepository laoRepo, ElectionRepository electionRepository) {
    this.laoRepo = laoRepo;
    this.messageRepo = messageRepo;
    this.electionRepository = electionRepository;
  }

  /**
   * Process an ElectionSetup message.
   *
   * @param context the HandlerContext of the message
   * @param electionSetup the message that was received
   */
  public void handleElectionSetup(HandlerContext context, ElectionSetup electionSetup)
      throws UnknownLaoException, InvalidChannelException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    if (!channel.isLaoChannel()) {
      throw new InvalidChannelException(electionSetup, "an lao channel", channel);
    }

    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    logger.debug("handleElectionSetup: channel " + channel + " name " + electionSetup.getName());

    Election election =
        new Election.ElectionBuilder(
                laoView.getId(), electionSetup.getCreation(), electionSetup.getName())
            .setElectionVersion(electionSetup.getElectionVersion())
            .setElectionQuestions(electionSetup.getQuestions())
            .setStart(electionSetup.getStartTime())
            .setEnd(electionSetup.getEndTime())
            .setState(CREATED)
            .build();

    // Add new election to repository
    electionRepository.updateElection(election);

    // Once the election is created, we subscribe to the election channel
    context
        .getMessageSender()
        .subscribe(election.getChannel())
        .doOnError(
            err -> logger.error("An error occurred while subscribing to election channel", err))
        .onErrorComplete()
        .subscribe();

    logger.debug("election id " + election.getId());

    Lao lao = laoView.createLaoCopy();
    lao.updateWitnessMessage(messageId, electionSetupWitnessMessage(messageId, election));
    laoRepo.updateLao(lao);
  }

  /**
   * Process an ElectionResult message.
   *
   * @param context the HandlerContext of the message
   * @param electionResult the message that was received
   */
  public void handleElectionResult(HandlerContext context, ElectionResult electionResult)
      throws UnknownElectionException {
    Channel channel = context.getChannel();

    logger.debug("handling election result");

    List<ElectionResultQuestion> resultsQuestions = electionResult.getElectionQuestionResults();
    logger.debug("size of resultsQuestions is " + resultsQuestions.size());
    // No need to check here that resultsQuestions is not empty, as it is already done at the
    // creation of the ElectionResult Data

    Election election =
        electionRepository
            .getElectionByChannel(channel)
            .builder()
            .setResults(computeResults(resultsQuestions))
            .setState(RESULTS_READY)
            .build();

    electionRepository.updateElection(election);
  }

  private Map<String, Set<QuestionResult>> computeResults(
      @NonNull List<ElectionResultQuestion> electionResultsQuestions) {

    Map<String, Set<QuestionResult>> results = new HashMap<>();

    for (ElectionResultQuestion resultQuestion : electionResultsQuestions) {
      results.put(resultQuestion.getId(), resultQuestion.getResult());
    }

    return results;
  }

  /**
   * Process an OpenElection message.
   *
   * @param context the HandlerContext of the message
   * @param openElection the message that was received
   */
  @SuppressWarnings("unused")
  public void handleElectionOpen(HandlerContext context, OpenElection openElection)
      throws InvalidStateException, UnknownElectionException {
    Channel channel = context.getChannel();

    logger.debug("handleOpenElection: channel " + channel);
    Election election = electionRepository.getElectionByChannel(channel);

    // If the state is not created, then this message is invalid
    if (election.getState() != CREATED) {
      throw new InvalidStateException(
          openElection, "election", election.getState().name(), CREATED.name());
    }

    // Sets the start time to now
    Election updated =
        election.builder().setState(OPENED).setStart(openElection.getOpenedAt()).build();

    logger.debug("election opened " + updated.getStartTimestamp());
    electionRepository.updateElection(updated);
  }

  /**
   * Process an ElectionEnd message.
   *
   * @param context the HandlerContext of the message
   * @param electionEnd the message that was received
   */
  @SuppressWarnings("unused")
  public void handleElectionEnd(HandlerContext context, ElectionEnd electionEnd)
      throws UnknownElectionException {
    Channel channel = context.getChannel();

    logger.debug("handleElectionEnd: channel " + channel);
    Election election =
        electionRepository.getElectionByChannel(channel).builder().setState(CLOSED).build();

    electionRepository.updateElection(election);
  }

  /**
   * Process a CastVote message.
   *
   * @param context the HandlerContext of the message
   * @param castVote the message that was received
   */
  public void handleCastVote(HandlerContext context, CastVote castVote)
      throws UnknownElectionException, DataHandlingException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    logger.debug("handleCastVote: channel " + channel);
    Election election = electionRepository.getElectionByChannel(channel);
    // Verify the vote was created before the end of the election or the election is not closed yet
    if (election.getEndTimestamp() >= castVote.getCreation() || election.getState() != CLOSED) {
      // Retrieve previous cast vote message stored for the given sender
      MessageID previousMessageId = election.getMessageMap().get(senderPk);

      // No previous message, we always handle it
      if (previousMessageId == null) {
        updateElectionWithVotes(castVote, messageId, senderPk, election);
        return;
      }

      // Retrieve previous message and make sure it is a CastVote
      Data previousData = messageRepo.getMessage(previousMessageId).getData();
      if (previousData == null) {
        throw new IllegalStateException(
            "The message corresponding to " + messageId + " does not exist");
      }

      if (!(previousData instanceof CastVote)) {
        throw new DataHandlingException(
            previousData, "The previous message of a cast vote was not a CastVote");
      }

      CastVote previousCastVote = (CastVote) previousData;

      // Verify the current cast vote message is the last one received
      if (previousCastVote.getCreation() <= castVote.getCreation()) {
        updateElectionWithVotes(castVote, messageId, senderPk, election);
      }
    }
  }

  private void updateElectionWithVotes(
      CastVote castVote, MessageID messageId, PublicKey senderPk, Election election) {
    Election updated =
        election
            .builder()
            .updateMessageMap(senderPk, messageId)
            .updateVotes(senderPk, castVote.getVotes())
            .build();

    electionRepository.updateElection(updated);
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

  /**
   * Simple way to handle a election key, add the given key to the given election
   *
   * @param context context
   * @param electionKey key to add
   */
  public void handleElectionKey(HandlerContext context, ElectionKey electionKey)
      throws UnknownElectionException {
    Channel channel = context.getChannel();

    logger.debug("handleElectionKey: channel " + channel);

    Election election =
        electionRepository
            .getElectionByChannel(channel)
            .builder()
            .setElectionKey(electionKey.getElectionVoteKey())
            .build();

    electionRepository.updateElection(election);

    logger.debug("handleElectionKey: election key has been set ");
  }
}
