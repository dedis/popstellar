package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

public final class ConsensusHandler {

  private static final Logger logger = LogManager.getLogger(ConsensusHandler.class);

  private final LAORepository laoRepo;

  @Inject
  public ConsensusHandler(LAORepository laoRepo) {
    this.laoRepo = laoRepo;
  }

  /**
   * Process an Elect message.
   *
   * @param context the HandlerContext of the message
   * @param consensusElect the data of the message that was received
   */
  public void handleElect(HandlerContext context, ConsensusElect consensusElect)
      throws UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    logger.debug("handleElect: " + channel + " id " + consensusElect.getInstanceId());

    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    Set<PublicKey> nodes = laoView.getWitnesses();
    nodes.add(laoView.getOrganizer());

    ElectInstance electInstance =
        new ElectInstance(messageId, channel, senderPk, nodes, consensusElect);
    Lao lao = laoView.createLaoCopy();
    lao.updateElectInstance(electInstance);

    laoRepo.updateLao(lao);
    laoRepo.updateNodes(laoView.getChannel());
  }

  public void handleElectAccept(HandlerContext context, ConsensusElectAccept consensusElectAccept)
      throws DataHandlingException, UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    logger.debug("handleElectAccept: " + channel + " id " + consensusElectAccept.getInstanceId());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Optional<ElectInstance> electInstanceOpt =
        laoView.getElectInstance(consensusElectAccept.getMessageId());
    if (!electInstanceOpt.isPresent()) {
      logger.warn("elect_accept for invalid messageId : " + consensusElectAccept.getMessageId());
      throw new InvalidMessageIdException(
          consensusElectAccept, consensusElectAccept.getMessageId());
    }

    ElectInstance electInstance = electInstanceOpt.get();
    electInstance.addElectAccept(senderPk, messageId, consensusElectAccept);
    Lao lao = laoView.createLaoCopy();
    lao.updateElectInstance(electInstance);

    laoRepo.updateLao(lao);
    laoRepo.updateNodes(laoView.getChannel());
  }

  @SuppressWarnings("unused")
  public <T extends Data> void handleBackend(HandlerContext context, T data) {
    logger.warn("Received a consensus message only for backend with action=" + data.getAction());
  }

  public void handleLearn(HandlerContext context, ConsensusLearn consensusLearn)
      throws DataHandlingException, UnknownLaoException {
    Channel channel = context.getChannel();

    logger.debug("handleLearn: " + channel + " id " + consensusLearn.getInstanceId());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Optional<ElectInstance> electInstanceOpt =
        laoView.getElectInstance(consensusLearn.getMessageId());
    if (!electInstanceOpt.isPresent()) {
      logger.warn("learn for invalid messageId : " + consensusLearn.getMessageId());
      throw new InvalidMessageIdException(consensusLearn, consensusLearn.getMessageId());
    }

    ElectInstance electInstance = electInstanceOpt.get();

    if (consensusLearn.getLearnValue().isDecision()) {
      electInstance.setState(ElectInstance.State.ACCEPTED);
    }
    Lao lao = laoView.createLaoCopy();
    lao.updateElectInstance(electInstance);

    laoRepo.updateLao(lao);
    laoRepo.updateNodes(laoView.getChannel());
  }

  public void handleConsensusFailure(HandlerContext context, ConsensusFailure failure)
      throws UnknownLaoException, InvalidMessageIdException {
    Channel channel = context.getChannel();

    logger.debug("handleConsensusFailure: " + channel + " id " + failure.getInstanceId());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Optional<ElectInstance> electInstanceOpt = laoView.getElectInstance(failure.getMessageId());
    if (!electInstanceOpt.isPresent()) {
      logger.warn("Failure for invalid messageId : " + failure.getMessageId());
      throw new InvalidMessageIdException(failure, failure.getMessageId());
    }

    ElectInstance electInstance = electInstanceOpt.get();
    electInstance.setState(ElectInstance.State.FAILED);
    Lao lao = laoView.createLaoCopy();
    lao.updateElectInstance(electInstance);

    laoRepo.updateLao(lao);
    laoRepo.updateNodes(laoView.getChannel());
  }
}
