package com.github.dedis.popstellar.utility.handler;

import android.util.Log;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.event.EventState;
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
    Set<String> acceptors = new HashSet<>(lao.getWitnesses());
    acceptors.add(lao.getOrganizer());

    Consensus consensus =
        new Consensus(
            consensusElect.getCreation(), consensusElect.getKey(), consensusElect.getValue());

    consensus.setMessageId(messageId);
    consensus.setProposer(senderPk);
    consensus.setChannel(channel);
    consensus.setEnd(Long.MAX_VALUE);
    consensus.setAcceptors(acceptors);
    consensus.setEventState(EventState.OPENED);

    lao.updateConsensus(consensus.getId(), consensus);

    return false;
  }

  public static boolean handleConsensusElectAccept(
      LAORepository laoRepository,
      String channel,
      ConsensusElectAccept consensusElectAccept,
      String messageId,
      String senderPk) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<Consensus> consensusOpt =
        getConsensusByMessageId(laoRepository, lao, consensusElectAccept.getMessageId());
    if (!consensusOpt.isPresent()) {
      return true;
    }

    Consensus consensus = consensusOpt.get();
    consensus.putAcceptorResponse(senderPk, messageId, consensusElectAccept.isAccept());

    lao.updateConsensus(consensus.getId(), consensus);

    return false;
  }

  public static boolean handleConsensusLearn(
      LAORepository laoRepository, String channel, ConsensusLearn consensusLearn) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<Consensus> consensusOpt =
        getConsensusByMessageId(laoRepository, lao, consensusLearn.getMessageId());

    if (!consensusOpt.isPresent()) {
      return true;
    }

    Consensus consensus = consensusOpt.get();

    //TODO check acceptors message_ids ?

    consensus.setAccepted(true);
    consensus.setEventState(EventState.RESULTS_READY);
    consensus.setEnd(System.currentTimeMillis() / 1000L);

    lao.updateConsensus(consensus.getId(), consensus);

    return false;
  }

  private static Optional<Consensus> getConsensusByMessageId(
      LAORepository laoRepository, Lao lao, String messageId) {
    MessageGeneral createMsg = laoRepository.getMessageById().get(messageId);

    if (createMsg == null || !(createMsg.getData() instanceof ConsensusElect)) {
      Log.d(TAG, "Invalid message id for a CreateConsensus : " + messageId);
      return Optional.empty();
    }

    ConsensusElect consensusElect = (ConsensusElect) createMsg.getData();
    String consensusId = consensusElect.getInstanceId();
    Optional<Consensus> consensusOpt = lao.getConsensus(consensusId);

    if (!consensusOpt.isPresent()) {
      Log.d(TAG, "No consensus found for id : " + consensusId);
    }

    return consensusOpt;
  }
}
