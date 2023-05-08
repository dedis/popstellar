package com.github.dedis.popstellar.utility.handler.data;

import static com.github.dedis.popstellar.model.objects.event.EventState.*;

import androidx.annotation.NonNull;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.utility.error.*;
import java.util.*;
import javax.inject.Inject;
import timber.log.Timber;

/** Election messages handler class */
public final class ElectionHandler {

  public static final String TAG = ElectionHandler.class.getSimpleName();

  private final LAORepository laoRepo;
  private final MessageRepository messageRepo;
  private final ElectionRepository electionRepository;

  private static final String ELECTION_NAME = "Election Name : ";
  private static final String MESSAGE_ID = "Message ID : ";

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

    String laoId = electionSetup.getLaoId();
    if (!laoRepo.containsLao(laoId)) {
      throw new UnknownLaoException(laoId);
    }

    if (!channel.isLaoChannel()) {
      throw new InvalidChannelException(electionSetup, "an lao channel", channel);
    }

    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    Timber.tag(TAG)
        .d("handleElectionSetup: channel: %s, name: %s", channel, electionSetup.getName());

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
            err ->
                Timber.tag(TAG).e(err, "An error occurred while subscribing to election channel"))
        .onErrorComplete()
        .subscribe();

    Timber.tag(TAG).d("election id %s", election.getId());

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

    Timber.tag(TAG).d("handling election result");

    List<ElectionResultQuestion> resultsQuestions = electionResult.getElectionQuestionResults();
    Timber.tag(TAG).d("size of resultsQuestions is %d", resultsQuestions.size());
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
   * Process an ElectionOpen message.
   *
   * @param context the HandlerContext of the message
   * @param electionOpen the message that was received
   */
  @SuppressWarnings("unused")
  public void handleElectionOpen(HandlerContext context, ElectionOpen electionOpen)
      throws InvalidStateException, UnknownElectionException, UnknownLaoException {
    Channel channel = context.getChannel();

    Timber.tag(TAG).d("handleOpenElection: channel %s", channel);

    String laoId = electionOpen.getLaoId();
    if (!laoRepo.containsLao(laoId)) {
      throw new UnknownLaoException(laoId);
    }

    Election election = electionRepository.getElectionByChannel(channel);

    // If the state is not created, then this message is invalid
    if (election.getState() != CREATED) {
      throw new InvalidStateException(
          electionOpen, "election", election.getState().name(), CREATED.name());
    }

    // Sets the start time to now
    Election updated =
        election.builder().setState(OPENED).setStart(electionOpen.getOpenedAt()).build();

    Timber.tag(TAG).d("election opened %d", updated.getStartTimestamp());
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

    Timber.tag(TAG).d("handleElectionEnd: channel %s", channel);
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
      throws UnknownElectionException, DataHandlingException, UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Timber.tag(TAG).d("handleCastVote: channel %s", channel);

    String laoId = castVote.getLaoId();
    if (!laoRepo.containsLao(laoId)) {
      throw new UnknownLaoException(laoId);
    }

    // Election id validity is checked with this
    Election election = electionRepository.getElectionByChannel(channel);

    if (election.getCreation() > castVote.getCreation()) {
      throw new DataHandlingException(castVote, "vote cannot be older than election creation");
    }

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
        ELECTION_NAME
            + "\n"
            + election.getName()
            + "\n\n"
            + "Election ID : "
            + "\n"
            + election.getId()
            + "\n\n"
            + "Question : "
            + "\n"
            + election.getElectionQuestions().get(0).getQuestion()
            + "\n\n"
            + "Message ID : "
            + "\n"
            + messageId.getEncoded());
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

    Timber.tag(TAG).d("handleElectionKey: channel %s", channel);

    Election election =
        electionRepository
            .getElectionByChannel(channel)
            .builder()
            .setElectionKey(electionKey.getElectionVoteKey())
            .build();

    electionRepository.updateElection(election);

    Timber.tag(TAG).d("handleElectionKey: election key has been set");
  }
}
