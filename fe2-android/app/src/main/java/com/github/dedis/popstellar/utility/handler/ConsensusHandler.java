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

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ConsensusHandler {

  private ConsensusHandler() {
    throw new IllegalStateException("Utility class");
  }

  public static final String TAG = ConsensusHandler.class.getSimpleName();

  public static boolean handleConsensusMessage(
      LAORepository laoRepository, String channel, Data data, String messageId, String senderPk) {
    Log.d(TAG, "handle Consensus message");

    switch (Objects.requireNonNull(Action.find(data.getAction()))) {
      case ELECT:
        return handleConsensusElect(
            laoRepository, channel, (ConsensusElect) data, messageId, senderPk);
      case ELECT_ACCEPT:
        return handleConsensusElectAccept(
            laoRepository, channel, (ConsensusElectAccept) data, messageId, senderPk);
      case LEARN:
        return handleConsensusLearn(laoRepository, channel, (ConsensusLearn) data);
      default:
        return true;
    }
  }

  public static boolean handleConsensusElect(
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

    return false;
  }

  public static boolean handleConsensusElectAccept(
      LAORepository laoRepository,
      String channel,
      ConsensusElectAccept consensusElectAccept,
      String messageId,
      String senderPk) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<Consensus> consensusOpt = lao.getConsensus(consensusElectAccept.getMessageId());
    if (!consensusOpt.isPresent()) {
      Log.w(TAG, "elect-accept for invalid messageId : " + consensusElectAccept.getMessageId());
      return true;
    }

    Consensus consensus = consensusOpt.get();
    consensus.putAcceptorResponse(senderPk, messageId, consensusElectAccept.isAccept());

    if (consensus.canBeAccepted() && !consensus.isFailed()) {
      consensus.setAccepted(true);
    }

    lao.updateConsensus(consensus);

    return false;
  }

  public static boolean handleConsensusLearn(
      LAORepository laoRepository, String channel, ConsensusLearn consensusLearn) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<Consensus> consensusOpt = lao.getConsensus(consensusLearn.getMessageId());

    if (!consensusOpt.isPresent()) {
      Log.w(TAG, "learn for invalid messageId : " + consensusLearn.getMessageId());
      return true;
    }

    Consensus consensus = consensusOpt.get();

    consensus.setAccepted(true);

    lao.updateConsensus(consensus);

    return false;
  }
}
