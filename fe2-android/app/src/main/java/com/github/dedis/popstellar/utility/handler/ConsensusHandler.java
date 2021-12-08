package com.github.dedis.popstellar.utility.handler;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;
import com.github.dedis.popstellar.utility.error.UnhandledDataTypeException;
import com.github.dedis.popstellar.utility.error.UnknownDataActionException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class ConsensusHandler {

  private ConsensusHandler() {
    throw new IllegalStateException("Utility class");
  }

  public static final String TAG = ConsensusHandler.class.getSimpleName();

  /**
   * Process a Consensus message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   * @param data the data of the message that was received
   * @param messageId the ID of the message that was received
   * @param senderPk the public key of the sender of this message
   */
  public static void handleConsensusMessage(
      LAORepository laoRepository, String channel, Data data, String messageId, String senderPk)
      throws DataHandlingException {
    Log.d(TAG, "handle Consensus message");

    Action action = Action.find(data.getAction());
    if (action == null) throw new UnknownDataActionException(data);

    switch (action) {
      case ELECT:
        handleConsensusElect(laoRepository, channel, (ConsensusElect) data, messageId, senderPk);
        break;
      case ELECT_ACCEPT:
        handleConsensusElectAccept(
            laoRepository, channel, (ConsensusElectAccept) data, messageId, senderPk);
        break;
      case PREPARE:
      case PROMISE:
      case PROPOSE:
      case ACCEPT:
        Log.w(TAG, "Received a consensus message only for backend with action=" + data.getAction());
        break;
      case LEARN:
        handleConsensusLearn(laoRepository, channel, (ConsensusLearn) data);
        break;
      default:
        Log.w(TAG, "Invalid action for a consensus object : " + data.getAction());
        throw new UnhandledDataTypeException(data, action.getAction());
    }
  }

  public static void handleConsensusElect(
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

  public static void handleConsensusElectAccept(
      LAORepository laoRepository,
      String channel,
      ConsensusElectAccept consensusElectAccept,
      String messageId,
      String senderPk)
      throws DataHandlingException {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<Consensus> consensusOpt = lao.getConsensus(consensusElectAccept.getMessageId());
    if (!consensusOpt.isPresent()) {
      Log.w(TAG, "elect-accept for invalid messageId : " + consensusElectAccept.getMessageId());
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

  public static void handleConsensusLearn(
      LAORepository laoRepository, String channel, ConsensusLearn consensusLearn)
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
