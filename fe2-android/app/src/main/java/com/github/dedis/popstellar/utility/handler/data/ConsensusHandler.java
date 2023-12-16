package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.WitnessingRepository;
import com.github.dedis.popstellar.utility.error.*;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import timber.log.Timber;

public final class ConsensusHandler {

  public static final String TAG = ConsensusHandler.class.getSimpleName();

  private final LAORepository laoRepo;
  private final WitnessingRepository witnessingRepo;

  @Inject
  public ConsensusHandler(LAORepository laoRepo, WitnessingRepository witnessingRepo) {
    this.laoRepo = laoRepo;
    this.witnessingRepo = witnessingRepo;
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

    Timber.tag(TAG).d("handleElect: channel: %s, id: %s", channel, consensusElect.instanceId);

    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    Set<PublicKey> nodes = witnessingRepo.getWitnesses(laoView.getId());
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

    Timber.tag(TAG)
        .d("handleElectAccept: channel: %s, id: %s", channel, consensusElectAccept.instanceId);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Optional<ElectInstance> electInstanceOpt =
        laoView.getElectInstance(consensusElectAccept.messageId);
    if (!electInstanceOpt.isPresent()) {
      Timber.tag(TAG).w("elect_accept for invalid messageId : %s", consensusElectAccept.messageId);
      throw new InvalidMessageIdException(consensusElectAccept, consensusElectAccept.messageId);
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
    Timber.tag(TAG)
        .w("Received a consensus message only for backend with action: %s", data.getAction());
  }

  public void handleLearn(HandlerContext context, ConsensusLearn consensusLearn)
      throws DataHandlingException, UnknownLaoException {
    Channel channel = context.getChannel();

    Timber.tag(TAG).d("handleLearn: channel: %s, id: %s", channel, consensusLearn.instanceId);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Optional<ElectInstance> electInstanceOpt = laoView.getElectInstance(consensusLearn.messageId);
    if (!electInstanceOpt.isPresent()) {
      Timber.tag(TAG).w("learn for invalid messageId : %s", consensusLearn.messageId);
      throw new InvalidMessageIdException(consensusLearn, consensusLearn.messageId);
    }

    ElectInstance electInstance = electInstanceOpt.get();

    if (consensusLearn.learnValue.isDecision()) {
      electInstance.state = ElectInstance.State.ACCEPTED;
    }
    Lao lao = laoView.createLaoCopy();
    lao.updateElectInstance(electInstance);

    laoRepo.updateLao(lao);
    laoRepo.updateNodes(laoView.getChannel());
  }

  public void handleConsensusFailure(HandlerContext context, ConsensusFailure failure)
      throws UnknownLaoException, InvalidMessageIdException {
    Channel channel = context.getChannel();

    Timber.tag(TAG).d("handleConsensusFailure: channel: %s, id: %s", channel, failure.instanceId);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Optional<ElectInstance> electInstanceOpt = laoView.getElectInstance(failure.messageId);
    if (!electInstanceOpt.isPresent()) {
      Timber.tag(TAG).w("Failure for invalid messageId : %s", failure.messageId);
      throw new InvalidMessageIdException(failure, failure.messageId);
    }

    ElectInstance electInstance = electInstanceOpt.get();
    electInstance.state = ElectInstance.State.FAILED;
    Lao lao = laoView.createLaoCopy();
    lao.updateElectInstance(electInstance);

    laoRepo.updateLao(lao);
    laoRepo.updateNodes(laoView.getChannel());
  }
}
