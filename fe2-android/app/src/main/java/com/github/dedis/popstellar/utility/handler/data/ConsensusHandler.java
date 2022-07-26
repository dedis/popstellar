package com.github.dedis.popstellar.utility.handler.data;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;

import java.util.*;

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
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Lao lao = laoRepository.getLaoByChannel(channel);
    Set<PublicKey> nodes = new HashSet<>(lao.getWitnesses());
    nodes.add(lao.getOrganizer());

    ElectInstance electInstance =
        new ElectInstance(messageId, channel, senderPk, nodes, consensusElect);
    lao.updateElectInstance(electInstance);
    laoRepository.updateNodes(lao.getChannel());
  }

  public static void handleElectAccept(
      HandlerContext context, ConsensusElectAccept consensusElectAccept)
      throws DataHandlingException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<ElectInstance> electInstanceOpt =
        lao.getElectInstance(consensusElectAccept.getMessageId());
    if (!electInstanceOpt.isPresent()) {
      Log.w(TAG, "elect_accept for invalid messageId : " + consensusElectAccept.getMessageId());
      throw new InvalidMessageIdException(
          consensusElectAccept, consensusElectAccept.getMessageId());
    }

    ElectInstance electInstance = electInstanceOpt.get();
    electInstance.addElectAccept(senderPk, messageId, consensusElectAccept);

    lao.updateElectInstance(electInstance);
    laoRepository.updateNodes(lao.getChannel());
  }

  @SuppressWarnings("unused")
  public static <T extends Data> void handleBackend(HandlerContext context, T data) {
    Log.w(TAG, "Received a consensus message only for backend with action=" + data.getAction());
  }

  public static void handleLearn(HandlerContext context, ConsensusLearn consensusLearn)
      throws DataHandlingException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();

    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<ElectInstance> electInstanceOpt = lao.getElectInstance(consensusLearn.getMessageId());

    if (!electInstanceOpt.isPresent()) {
      Log.w(TAG, "learn for invalid messageId : " + consensusLearn.getMessageId());
      throw new InvalidMessageIdException(consensusLearn, consensusLearn.getMessageId());
    }

    ElectInstance electInstance = electInstanceOpt.get();

    if (consensusLearn.getLearnValue().isDecision()) {
      electInstance.setState(ElectInstance.State.ACCEPTED);
    }
    lao.updateElectInstance(electInstance);
    laoRepository.updateNodes(lao.getChannel());
  }

  public static void handleConsensusFailure(HandlerContext context, ConsensusFailure failure)
      throws InvalidMessageIdException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();

    Lao lao = laoRepository.getLaoByChannel(channel);
    Optional<ElectInstance> electInstanceOpt = lao.getElectInstance(failure.getMessageId());

    if (!electInstanceOpt.isPresent()) {
      Log.w(TAG, "Failure for invalid messageId : " + failure.getMessageId());
      throw new InvalidMessageIdException(failure, failure.getMessageId());
    }

    ElectInstance electInstance = electInstanceOpt.get();

    electInstance.setState(ElectInstance.State.FAILED);
    lao.updateElectInstance(electInstance);
    laoRepository.updateNodes(lao.getChannel());
  }
}
