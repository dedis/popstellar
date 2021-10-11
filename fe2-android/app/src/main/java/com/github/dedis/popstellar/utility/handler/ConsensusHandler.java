package com.github.dedis.popstellar.utility.handler;

import android.util.Log;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusVote;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.CreateConsensus;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.LearnConsensus;
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
      case PHASE_1_ELECT:
        return handleConsensusCreate(laoRepository, channel, (CreateConsensus) data, messageId, senderPk);
      case PHASE_1_ELECT_ACCEPT:
        return handleConsensusVote(laoRepository, channel, (ConsensusVote) data, messageId, senderPk);
      case PHASE_1_LEARN:
        return handleConsensusLearn(laoRepository, channel, (LearnConsensus) data);
      default:
        return true;
    }
  }

  public static boolean handleConsensusCreate(
      LAORepository laoRepository,
      String channel,
      CreateConsensus createConsensus,
      String messageId,
      String senderPk
  ) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Set<String> acceptors = new HashSet<>(lao.getWitnesses());
    acceptors.add(lao.getOrganizer());

    Consensus consensus = new Consensus(
      createConsensus.getCreation(),
      createConsensus.getKey(),
      createConsensus.getValue()
    );

    consensus.setMessageId(messageId);
    consensus.setProposer(senderPk);
    consensus.setChannel(channel);
    consensus.setEnd(Long.MAX_VALUE);
    consensus.setAcceptors(acceptors);
    consensus.setEventState(EventState.OPENED);

    lao.updateConsensus(consensus.getId(), consensus);

    return false;
  }

  public static boolean handleConsensusVote(
      LAORepository laoRepository,
      String channel,
      ConsensusVote consensusVote,
      String messageId,
      String senderPk
  ) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<Consensus> consensusOpt = getConsensusByMessageId(laoRepository, lao, consensusVote.getMessageId());
    if (!consensusOpt.isPresent()) {
      return true;
    }

    Consensus consensus = consensusOpt.get();
    consensus.putAcceptorResponse(senderPk, messageId, consensusVote.isAccept());

    lao.updateConsensus(consensus.getId(), consensus);

    return false;
  }

  public static boolean handleConsensusLearn(
      LAORepository laoRepository,
      String channel,
      LearnConsensus learnConsensus
  ) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<Consensus> consensusOpt = getConsensusByMessageId(laoRepository, lao, learnConsensus.getMessageId());

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

  private static Optional<Consensus> getConsensusByMessageId(LAORepository laoRepository, Lao lao, String messageId) {
    MessageGeneral createMsg = laoRepository.getMessageById().get(messageId);

    if (createMsg == null || !(createMsg.getData() instanceof CreateConsensus)) {
      Log.d(TAG, "Invalid message id for a CreateConsensus : " + messageId);
      return Optional.empty();
    }

    CreateConsensus createConsensus = (CreateConsensus) createMsg.getData();
    String consensusId = createConsensus.getInstanceId();
    Optional<Consensus> consensusOpt = lao.getConsensus(consensusId);

    if (!consensusOpt.isPresent()) {
      Log.d(TAG, "No consensus found for id : " + consensusId);
    }

    return consensusOpt;
  }
}
