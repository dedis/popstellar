package com.github.dedis.popstellar.utility.handler;

import android.util.Log;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusVote;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.CreateConsensus;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.repository.LAORepository;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ConsensusHandler {

  private ConsensusHandler() {
    throw new IllegalStateException("Utility class");
  }

  public static final String TAG = ConsensusHandler.class.getSimpleName();

  public static boolean handleConsensusMessage(
      LAORepository laoRepository,
      String channel,
      Data data,
      String messageId,
      String senderPk
  ) {
    Log.d(TAG, "handle Consensus message");

    switch (Objects.requireNonNull(Action.find(data.getAction()))) {
      case PHASE_1_ELECT:
        return handleConsensusCreate(laoRepository, channel, (CreateConsensus) data, senderPk);
      case PHASE_1_ELECT_ACCEPT:
        return handleConsensusVote(laoRepository, channel, (ConsensusVote) data, senderPk);
      case PHASE_1_LEARN:
        //TODO add learn handler
      default:
        return true;
    }
  }

  public static boolean handleConsensusCreate(
      LAORepository laoRepository,
      String channel,
      CreateConsensus createConsensus,
      String senderPk
  ) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Set<String> acceptors = new HashSet<>(lao.getWitnesses());
    acceptors.add(lao.getOrganizer());

    Consensus consensus = new Consensus(
      createConsensus.getType(),
      createConsensus.getObjId(),
      createConsensus.getCreation(),
      createConsensus.getProperty(),
      createConsensus.getValue()
    );

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
      String senderPk
  ) {
    Lao lao = laoRepository.getLaoByChannel(channel);

    //TODO replace messageId by consensusId ?
    Optional<Consensus> consensusOpt = lao.getConsensus(consensusVote.getMessageId());
    if (!consensusOpt.isPresent()) {
      return true;
    }

    Consensus consensus = consensusOpt.get();
    consensus.putAcceptorResponse(senderPk, consensusVote.isAccept());

    //Part 1 : all acceptors need to accept
    long countAccepted = consensus.getAcceptorsResponses().values().stream().filter(b -> b).count();
    boolean isAccepted = countAccepted == consensus.getAcceptors().size();

    consensus.setAccepted(isAccepted);
    if (isAccepted) {
      consensus.setEventState(EventState.RESULTS_READY);
    }
    lao.updateConsensus(consensus.getId(), consensus);

    return false;
  }
}
