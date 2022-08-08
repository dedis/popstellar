package com.github.dedis.popstellar.utility.handler.data;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.*;

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
  public static void handleElect(HandlerContext context, ConsensusElect consensusElect)
      throws UnknownLaoException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Log.d(TAG, "handleElect: " + channel + " id " + consensusElect.getInstanceId());
    Optional<LaoView> laoViewOptional = laoRepository.getLaoViewByChannel(channel);
    if (!laoViewOptional.isPresent()) {
      throw new UnknownLaoException(channel.extractLaoId());
    }
    LaoView laoView = laoViewOptional.get();

    Set<PublicKey> nodes = laoView.getWitnesses();
    nodes.add(laoView.getOrganizer());

    ElectInstance electInstance =
        new ElectInstance(messageId, channel, senderPk, nodes, consensusElect);
    Lao lao = laoView.getLao();
    lao.updateElectInstance(electInstance);

    laoRepository.updateNodes(laoView.getChannel());
    laoRepository.updateLao(lao);
  }

  public static void handleElectAccept(
      HandlerContext context, ConsensusElectAccept consensusElectAccept)
      throws DataHandlingException, UnknownLaoException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Log.d(TAG, "handleElectAccept: " + channel + " id " + consensusElectAccept.getInstanceId());
    Optional<LaoView> laoViewOptional = laoRepository.getLaoViewByChannel(channel);
    if (!laoViewOptional.isPresent()) {
      throw new UnknownLaoException(channel.extractLaoId());
    }
    LaoView laoView = laoViewOptional.get();

    Optional<ElectInstance> electInstanceOpt =
        laoView.getElectInstance(consensusElectAccept.getMessageId());
    if (!electInstanceOpt.isPresent()) {
      Log.w(TAG, "elect_accept for invalid messageId : " + consensusElectAccept.getMessageId());
      throw new InvalidMessageIdException(
          consensusElectAccept, consensusElectAccept.getMessageId());
    }

    ElectInstance electInstance = electInstanceOpt.get();
    electInstance.addElectAccept(senderPk, messageId, consensusElectAccept);
    Lao lao = laoView.getLao();
    lao.updateElectInstance(electInstance);

    laoRepository.updateLao(lao);
    laoRepository.updateNodes(laoView.getChannel());
  }

  @SuppressWarnings("unused")
  public static <T extends Data> void handleBackend(HandlerContext context, T data) {
    Log.w(TAG, "Received a consensus message only for backend with action=" + data.getAction());
  }

  public static void handleLearn(HandlerContext context, ConsensusLearn consensusLearn)
      throws DataHandlingException, UnknownLaoException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();

    Log.d(TAG, "handleLearn: " + channel + " id " + consensusLearn.getInstanceId());
    Optional<LaoView> laoViewOptional = laoRepository.getLaoViewByChannel(channel);
    if (!laoViewOptional.isPresent()) {
      throw new UnknownLaoException(channel.extractLaoId());
    }
    LaoView laoView = laoViewOptional.get();

    Optional<ElectInstance> electInstanceOpt =
        laoView.getElectInstance(consensusLearn.getMessageId());
    if (!electInstanceOpt.isPresent()) {
      Log.w(TAG, "learn for invalid messageId : " + consensusLearn.getMessageId());
      throw new InvalidMessageIdException(consensusLearn, consensusLearn.getMessageId());
    }

    ElectInstance electInstance = electInstanceOpt.get();

    if (consensusLearn.getLearnValue().isDecision()) {
      electInstance.setState(ElectInstance.State.ACCEPTED);
    }
    Lao lao = laoView.getLao();
    lao.updateElectInstance(electInstance);

    laoRepository.updateLao(lao);
    laoRepository.updateNodes(laoView.getChannel());
  }

  public static void handleConsensusFailure(HandlerContext context, ConsensusFailure failure)
      throws UnknownLaoException, InvalidMessageIdException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();

    Log.d(TAG, "handleConsensusFailure: " + channel + " id " + failure.getInstanceId());
    Optional<LaoView> laoViewOptional = laoRepository.getLaoViewByChannel(channel);
    if (!laoViewOptional.isPresent()) {
      throw new UnknownLaoException(channel.extractLaoId());
    }
    LaoView laoView = laoViewOptional.get();

    Optional<ElectInstance> electInstanceOpt = laoView.getElectInstance(failure.getMessageId());
    if (!electInstanceOpt.isPresent()) {
      Log.w(TAG, "Failure for invalid messageId : " + failure.getMessageId());
      throw new InvalidMessageIdException(failure, failure.getMessageId());
    }

    ElectInstance electInstance = electInstanceOpt.get();
    electInstance.setState(ElectInstance.State.FAILED);
    Lao lao = laoView.getLao();
    lao.updateElectInstance(electInstance);

    laoRepository.updateLao(lao);
    laoRepository.updateNodes(laoView.getChannel());
  }
}
