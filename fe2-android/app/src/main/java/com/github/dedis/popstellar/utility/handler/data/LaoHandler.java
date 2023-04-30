package com.github.dedis.popstellar.utility.handler.data;

import android.annotation.SuppressLint;

import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.lao.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.security.KeyManager;

import java.util.*;

import javax.inject.Inject;

import io.reactivex.exceptions.Exceptions;
import timber.log.Timber;

/** Lao messages handler class */
public final class LaoHandler {

  public static final String TAG = LaoHandler.class.getSimpleName();

  private final MessageRepository messageRepo;
  private final LAORepository laoRepo;
  private final KeyManager keyManager;
  private final ServerRepository serverRepo;

  @Inject
  public LaoHandler(
      KeyManager keyManager,
      MessageRepository messageRepo,
      LAORepository laoRepo,
      ServerRepository serverRepo) {
    this.messageRepo = messageRepo;
    this.laoRepo = laoRepo;
    this.keyManager = keyManager;
    this.serverRepo = serverRepo;
  }

  /**
   * Process a CreateLao message.
   *
   * @param context the HandlerContext of the message
   * @param createLao the message that was received
   */
  @SuppressLint("CheckResult") // for now concerns Consensus which is not a priority this semester
  public void handleCreateLao(HandlerContext context, CreateLao createLao)
      throws UnknownLaoException {
    Channel channel = context.getChannel();

    Timber.tag(TAG).d("handleCreateLao: channel: %s, msg: %s", channel, createLao);
    Lao lao = new Lao(createLao.getId());

    lao.setName(createLao.getName());
    lao.setCreation(createLao.getCreation());
    lao.setLastModified(createLao.getCreation());
    lao.setOrganizer(createLao.getOrganizer());
    lao.setId(createLao.getId());
    lao.setWitnesses(new HashSet<>(createLao.getWitnesses()));

    laoRepo.updateLao(lao);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    PublicKey publicKey = keyManager.getMainPublicKey();
    if (laoView.isOrganizer(publicKey) || laoView.isWitness(publicKey)) {
      context
          .getMessageSender()
          .subscribe(lao.getChannel().subChannel("consensus"))
          .subscribe( // For now if we receive an error, we assume that it is because the server
              // running is the scala one which does not implement consensus
              () -> Timber.tag(TAG).d("subscription to consensus channel was a success"),
              error ->
                  Timber.tag(TAG).d(error, "error while trying to subscribe to consensus channel"));
    }

    /* Creation channel coin*/
    context
        .getMessageSender()
        .subscribe(channel.subChannel("coin"))
        .subscribe(
            () -> Timber.tag(TAG).d("subscription to the coin channel was a success"),
            error -> Timber.tag(TAG).d(error, "error while trying  to subscribe to coin channel"));

    laoRepo.updateNodes(channel);
  }

  /**
   * Process an UpdateLao message.
   *
   * @param context the HandlerContext of the message
   * @param updateLao the message that was received
   */
  public void handleUpdateLao(HandlerContext context, UpdateLao updateLao)
      throws DataHandlingException, UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Timber.tag(TAG).d("Receive Update Lao Broadcast msg: %s", updateLao);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    if (laoView.getLastModified() > updateLao.getLastModified()) {
      // the current state we have is more up to date
      throw new DataHandlingException(
          updateLao, "The current Lao is more up to date than the update lao message");
    }

    WitnessMessage message;
    if (!updateLao.getName().equals(laoView.getName())) {
      message = updateLaoNameWitnessMessage(messageId, updateLao, laoView);
    } else if (!laoView.areWitnessSetsEqual(updateLao.getWitnesses())) {
      message = updateLaoWitnessesWitnessMessage(messageId, updateLao, laoView);
    } else {
      Timber.tag(TAG).d("Cannot set the witness message title to update lao");
      throw new DataHandlingException(
          updateLao, "Cannot set the witness message title to update lao");
    }

    Lao lao = laoView.createLaoCopy();
    lao.updateWitnessMessage(messageId, message);
    if (!laoView.isWitnessesEmpty()) {
      // We send a pending update only if there are already some witness that need to sign this
      // UpdateLao
      lao.addPendingUpdate(new PendingUpdate(updateLao.getLastModified(), messageId));
    }

    laoRepo.updateNodes(channel);
    laoRepo.updateLao(lao);
  }

  /**
   * Process a StateLao message.
   *
   * @param context the HandlerContext of the message
   * @param stateLao the message that was received
   */
  @SuppressLint("CheckResult")
  public void handleStateLao(HandlerContext context, StateLao stateLao)
      throws DataHandlingException, UnknownLaoException {
    Channel channel = context.getChannel();

    Timber.tag(TAG).d("Receive State Lao Broadcast msg: %s", stateLao);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Timber.tag(TAG).d("Receive State Lao Broadcast %s", stateLao.getName());

    messageRepo
        .isMessagePresent(stateLao.getModificationId(), true)
        .subscribe(
            isPresent -> {
              if (!isPresent) {
                Timber.tag(TAG).d("Can't find modification id : %s", stateLao.getModificationId());
                // queue it if we haven't received the update message yet
                throw new InvalidMessageIdException(stateLao, stateLao.getModificationId());
              } else {
                Timber.tag(TAG).d("Verifying signatures");
                for (PublicKeySignaturePair pair : stateLao.getModificationSignatures()) {
                  if (!pair.getWitness()
                      .verify(pair.getSignature(), stateLao.getModificationId())) {
                    throw new InvalidSignatureException(stateLao, pair.getSignature());
                  }
                }

                Timber.tag(TAG).d("Success to verify state lao signatures");

                // TODO: verify if lao/state_lao is consistent with the lao/update message

                Lao lao = laoView.createLaoCopy();
                lao.setId(stateLao.getId());
                lao.setWitnesses(stateLao.getWitnesses());
                lao.setName(stateLao.getName());
                lao.setLastModified(stateLao.getLastModified());
                lao.setModificationId(stateLao.getModificationId());

                PublicKey publicKey = keyManager.getMainPublicKey();
                if (laoView.isOrganizer(publicKey) || laoView.isWitness(publicKey)) {
                  context
                      .getMessageSender()
                      .subscribe(laoView.getChannel().subChannel("consensus"))
                      .subscribe(
                          () -> Timber.tag(TAG).d("Successful subscribe to consensus channel"),
                          e -> Timber.tag(TAG).d(e, "Unsuccessful subscribe to consensus channel"));
                }

                // Now we're going to remove all pending updates which came prior to this state lao
                long targetTime = stateLao.getLastModified();
                lao.getPendingUpdates()
                    .removeIf(pendingUpdate -> pendingUpdate.getModificationTime() <= targetTime);

                laoRepo.updateLao(lao);
                laoRepo.updateNodes(channel);
              }
            },
            Exceptions::propagate);
  }

  public static WitnessMessage updateLaoNameWitnessMessage(
      MessageID messageId, UpdateLao updateLao, LaoView laoView) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("Update Lao Name ");
    message.setDescription(
        "Old Name : "
            + laoView.getName()
            + "\n"
            + "New Name : "
            + updateLao.getName()
            + "\n"
            + "Message ID : "
            + messageId);
    return message;
  }

  public static WitnessMessage updateLaoWitnessesWitnessMessage(
      MessageID messageId, UpdateLao updateLao, LaoView laoView) {
    WitnessMessage message = new WitnessMessage(messageId);
    List<PublicKey> tempList = new ArrayList<>(updateLao.getWitnesses());
    message.setTitle("Update Lao Witnesses");
    message.setDescription(
        "Lao Name : "
            + laoView.getName()
            + "\n"
            + "Message ID : "
            + messageId
            + "\n"
            + "New Witness ID : "
            + tempList.get(tempList.size() - 1));
    return message;
  }

  public void handleGreetLao(HandlerContext context, GreetLao greetLao) throws UnknownLaoException {
    Channel channel = context.getChannel();

    Timber.tag(TAG).d("handleGreetLao: channel: %s, msg: %s", channel, greetLao);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    // Check the correctness of the LAO id
    if (!laoView.getId().equals(greetLao.getId())) {
      Timber.tag(TAG)
          .d(
              "Current lao id %s doesn't match the lao id from greetLao message (%s)",
              laoView.getId(), greetLao.getId());
      throw new IllegalArgumentException(
          "Current lao doesn't match the lao id from the greetLao message");
    }
    Timber.tag(TAG).d("Creating a server with IP: %s", greetLao.getAddress());

    Server server = new Server(greetLao.getAddress(), greetLao.getFrontendKey());

    Timber.tag(TAG).d("Adding the server to the repository for lao id : %s", laoView.getId());
    serverRepo.addServer(greetLao.getId(), server);

    // Extend the current connection by connecting to the peers of the main server
    // The greetLao will also be sent by the other servers, so the message sender
    // should handle this, avoiding to connect twice to the same server
    context.getMessageSender().extendConnection(greetLao.getPeers());
  }
}
