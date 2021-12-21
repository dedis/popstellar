package com.github.dedis.popstellar.utility.handler;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
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
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   * @param consensusElect the data of the message that was received
   * @param messageId the ID of the message that was received
   * @param senderPk the public key of the sender of this message
   */
  public static void handleElect(
      LAORepository laoRepository,
      String channel,
      ConsensusElect consensusElect,
      String messageId,
      String senderPk) {
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
      LAORepository laoRepository,
      String channel,
      ConsensusElectAccept consensusElectAccept,
      String messageId,
      String senderPk)
      throws DataHandlingException {
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

  public static <T extends Data> void handleBackend(
      LAORepository laoRepository, String channel, T data, String messageId, String senderPk) {
    Log.w(TAG, "Received a consensus message only for backend with action=" + data.getAction());
  }

  public static void handleLearn(
      LAORepository laoRepository,
      String channel,
      ConsensusLearn consensusLearn,
      String messageId,
      String senderPk)
      throws DataHandlingException {
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
}
