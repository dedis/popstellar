package com.github.dedis.popstellar.utility.handler.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MessageRepository;
import com.github.dedis.popstellar.utility.error.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import static com.github.dedis.popstellar.model.objects.event.EventState.*;

/** Election messages handler class */
public final class ElectionHandler {

  public static final String TAG = ElectionHandler.class.getSimpleName();

  private final LAORepository laoRepo;
  private final MessageRepository messageRepo;

  @Inject
  public ElectionHandler(MessageRepository messageRepo, LAORepository laoRepo) {
    this.laoRepo = laoRepo;
    this.messageRepo = messageRepo;
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
    Log.d(TAG, "handleElectionSetup: channel " + channel + " name " + electionSetup.getName());

    Election election =
        new Election.ElectionBuilder(
                laoView.getId(), electionSetup.getCreation(), electionSetup.getName())
            .setLaoChannel(channel)
            .setElectionVersion(electionSetup.getElectionVersion())
            .setElectionQuestions(electionSetup.getQuestions())
            .setStart(electionSetup.getStartTime())
            .setEnd(electionSetup.getEndTime())
            .setState(CREATED)
            .build();

    // Once the election is created, we subscribe to the election channel
    context.getMessageSender().subscribe(election.getChannel()).subscribe();
    Log.d(TAG, "election id " + election.getId());

    Lao lao = laoView.createLaoCopy();
    lao.updateElection(election.getId(), election);
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
      throws UnknownLaoException, DataHandlingException {
    Channel channel = context.getChannel();

    Log.d(TAG, "handling election result");

    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    Election election = laoRepo.getElectionByChannel(channel);

    List<ElectionResultQuestion> resultsQuestions = electionResult.getElectionQuestionResults();
    Log.d(TAG, "size of resultsQuestions is " + resultsQuestions.size());
    if (resultsQuestions.isEmpty())
      throw new DataHandlingException(electionResult, "the questions results is empty");

    Election updated =
        election
            .builder()
            .setResults(computeResults(resultsQuestions))
            .setState(RESULTS_READY)
            .build();

    Lao lao = laoView.createLaoCopy();
    lao.updateElection(updated.getId(), updated);

    laoRepo.updateLao(lao);
  }

  private Map<String, List<QuestionResult>> computeResults(
      @NonNull List<ElectionResultQuestion> electionResultsQuestions) {

    Map<String, List<QuestionResult>> results = new HashMap<>();

    for (ElectionResultQuestion resultQuestion : electionResultsQuestions) {
      results.put(
          resultQuestion.getId(),
          resultQuestion.getResult().stream()
              .sorted(Comparator.comparing(QuestionResult::getCount).reversed())
              .collect(Collectors.toList()));
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
      throws UnknownLaoException, InvalidStateException {
    Channel channel = context.getChannel();

    Log.d(TAG, "handleOpenElection: channel " + channel);

    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    Election election = laoRepo.getElectionByChannel(channel);

    // If the state is not created, then this message is invalid
    if (election.getState() != CREATED) {
      throw new InvalidStateException(
          openElection, "election", election.getState().name(), CREATED.name());
    }

    // Sets the start time to now
    Election updated =
        election.builder().setState(OPENED).setStart(Instant.now().getEpochSecond()).build();
    Log.d(TAG, "election opened " + updated.getStartTimestamp());
    Lao lao = laoView.createLaoCopy();
    lao.updateElection(updated.getId(), updated);

    laoRepo.updateLao(lao);
  }

  /**
   * Process an ElectionEnd message.
   *
   * @param context the HandlerContext of the message
   * @param electionEnd the message that was received
   */
  @SuppressWarnings("unused")
  public void handleElectionEnd(HandlerContext context, ElectionEnd electionEnd)
      throws UnknownLaoException {
    Channel channel = context.getChannel();

    Log.d(TAG, "handleElectionEnd: channel " + channel);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Election election = laoRepo.getElectionByChannel(channel).builder().setState(CLOSED).build();
    Lao lao = laoView.createLaoCopy();
    lao.updateElection(election.getId(), election);

    laoRepo.updateLao(lao);
  }

  /**
   * Process a CastVote message.
   *
   * @param context the HandlerContext of the message
   * @param castVote the message that was received
   */
  @SuppressWarnings("unchecked") // Because of the way CastVote is designed, this must be done
  public void handleCastVote(HandlerContext context, CastVote<?> castVote)
      throws UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Log.d(TAG, "handleCastVote: channel " + channel);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Election election = laoRepo.getElectionByChannel(channel);
    Lao lao = laoView.createLaoCopy();
    // Verify the vote was created before the end of the election or the election is not closed yet
    if (election.getEndTimestamp() >= castVote.getCreation() || election.getState() != CLOSED) {
      // Retrieve previous cast vote message stored for the given sender
      Optional<MessageID> previousMessageIdOption =
          election.getMessageMap().entrySet().stream()
              .filter(entry -> senderPk.equals(entry.getValue()))
              .map(Map.Entry::getKey)
              .findFirst();
      // Retrieve the creation time of the previous cast vote, if doesn't exist replace with min
      // Value
      long previousMessageCreation =
          previousMessageIdOption
              .map(messageRepo::getMessage)
              .map(MessageGeneral::getData)
              .map(CastVote.class::cast)
              .map(CastVote::getCreation)
              .orElse(Long.MIN_VALUE);

      // Verify the current cast vote message is the last one received
      if (previousMessageCreation <= castVote.getCreation()) {
        Election.ElectionBuilder builder = election.builder().updateMessageMap(senderPk, messageId);

        // Filter given the content of the vote
        if (election.getElectionVersion() == ElectionVersion.OPEN_BALLOT) {
          List<ElectionVote> votes = (List<ElectionVote>) castVote.getVotes();
          builder.updateOpenBallotVotesBySender(
              senderPk,
              votes.stream()
                  .sorted(Comparator.comparing(ElectionVote::getId))
                  .collect(Collectors.toList()));
        } else {
          List<ElectionEncryptedVote> votes = (List<ElectionEncryptedVote>) castVote.getVotes();
          builder.updateEncryptedVotesBySender(
              senderPk,
              votes.stream()
                  .sorted(Comparator.comparing(ElectionEncryptedVote::getId))
                  .collect(Collectors.toList()));
        }

        Election updated = builder.build();
        lao.updateElection(updated.getId(), updated);
      }
    }
    laoRepo.updateLao(lao);
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
      throws UnknownLaoException {
    Channel channel = context.getChannel();

    Log.d(TAG, "handleElectionKey: channel " + channel);

    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Election election =
        laoRepo
            .getElectionByChannel(channel)
            .builder()
            .setElectionKey(electionKey.getElectionVoteKey())
            .build();

    Lao lao = laoView.createLaoCopy();
    lao.updateElection(election.getId(), election);
    laoRepo.updateLao(lao);

    Log.d(TAG, "handleElectionKey: election key has been set ");
  }
}
