package com.github.dedis.popstellar.utility.handler.data;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusFailure;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class ConsensusHandler {

  private ConsensusHandler() {
    throw new IllegalStateException("Utility class");
  }

  public static final String TAG = ConsensusHandler.class.getSimpleName();

  /**
   * Process an Elect message.
   *
   * @param context the HandlerContext of the message
   * @param consensusElect the data of the message that was received
   */
  public static void handleElect(HandlerContext context, ConsensusElect consensusElect) {
    LAORepository laoRepository = context.getLaoRepository();
    String channel = context.getChannel();
    String messageId = context.getMessageId();
    String senderPk = context.getSenderPk();

    Lao lao = laoRepository.getLaoByChannel(channel);
    Set<String> nodes = new HashSet<>(lao.getWitnesses());
    nodes.add(lao.getOrganizer());

    Consensus consensus =
        new Consensus(
            consensusElect.getCreation(), consensusElect.getKey(), consensusElect.getValue());

    consensus.setMessageId(messageId);
    consensus.setProposer(senderPk);
    consensus.setChannel(channel);
    consensus.setNodes(nodes);

    lao.updateConsensus(consensus);
    laoRepository.updateNodes(lao.getChannel());
  }

  public static void handleElectAccept(
      HandlerContext context, ConsensusElectAccept consensusElectAccept)
      throws DataHandlingException {
    LAORepository laoRepository = context.getLaoRepository();
    String channel = context.getChannel();
    String messageId = context.getMessageId();
    String senderPk = context.getSenderPk();

    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<Consensus> consensusOpt = lao.getConsensus(consensusElectAccept.getMessageId());
    if (!consensusOpt.isPresent()) {
      Log.w(TAG, "elect_accept for invalid messageId : " + consensusElectAccept.getMessageId());
      throw new InvalidMessageIdException(
          consensusElectAccept, consensusElectAccept.getMessageId());
    }

    Consensus consensus = consensusOpt.get();
    if (consensusElectAccept.isAccept()) {
      consensus.putPositiveAcceptorResponse(senderPk, messageId);
    }

    lao.updateConsensus(consensus);
    laoRepository.updateNodes(lao.getChannel());
  }

  @SuppressWarnings("unused")
  public static <T extends Data> void handleBackend(HandlerContext context, T data) {
    Log.w(TAG, "Received a consensus message only for backend with action=" + data.getAction());
  }

  public static void handleLearn(HandlerContext context, ConsensusLearn consensusLearn)
      throws DataHandlingException {
    LAORepository laoRepository = context.getLaoRepository();
    String channel = context.getChannel();

    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<Consensus> consensusOpt = lao.getConsensus(consensusLearn.getMessageId());

    if (!consensusOpt.isPresent()) {
      Log.w(TAG, "learn for invalid messageId : " + consensusLearn.getMessageId());
      throw new InvalidMessageIdException(consensusLearn, consensusLearn.getMessageId());
    }

    Consensus consensus = consensusOpt.get();

    consensus.setAccepted(consensusLearn.getLearnValue().isDecision());
    lao.updateConsensus(consensus);
    laoRepository.updateNodes(lao.getChannel());
  }

  public static void handleConsensusFailure(HandlerContext context, ConsensusFailure failure)
      throws InvalidMessageIdException {
    LAORepository laoRepository = context.getLaoRepository();
    String channel = context.getChannel();

    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<Consensus> consensusOpt = lao.getConsensus(failure.getMessageId());

    if (!consensusOpt.isPresent()) {
      Log.w(TAG, "Failure for invalid messageId : " + failure.getMessageId());
      throw new InvalidMessageIdException(failure, failure.getMessageId());
    }

    Consensus consensus = consensusOpt.get();

    consensus.setFailed(true);
    lao.updateConsensus(consensus);
    laoRepository.updateNodes(lao.getChannel());
  }
}
