package com.github.dedis.popstellar.utility.handler.data;

import android.annotation.SuppressLint;
import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.lao.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.ServerRepository;
import com.github.dedis.popstellar.utility.error.*;

import java.util.*;

/** Lao messages handler class */
public final class LaoHandler {

  public static final String TAG = LaoHandler.class.getSimpleName();

  private LaoHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Process a CreateLao message.
   *
   * @param context the HandlerContext of the message
   * @param createLao the message that was received
   */
  @SuppressLint("CheckResult") // for now concerns Consensus which is not a priority this semester
  public static void handleCreateLao(HandlerContext context, CreateLao createLao) {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = new Channel(context.getChannel());

    Log.d(TAG, "handleCreateLao: channel " + channel + ", msg=" + createLao);
    Lao lao = new Lao(createLao.getId());

    lao.setName(createLao.getName());
    lao.setCreation(createLao.getCreation());
    lao.setLastModified(createLao.getCreation());
    lao.setOrganizer(createLao.getOrganizer());
    lao.setId(createLao.getId());
    lao.setWitnesses(new HashSet<>(createLao.getWitnesses()));

    // Adding the newly created LAO to the repository
    laoRepository.updateLao(lao);
    laoRepository.setAllLaoSubject();

    PublicKey publicKey = context.getKeyManager().getMainPublicKey();
    if (lao.getOrganizer().equals(publicKey) || lao.getWitnesses().contains(publicKey)) {
      context
          .getMessageSender()
          .subscribe(lao.getChannel().subChannel("consensus"))
          .subscribe( // For now if we receive an error, we assume that it is because the server
              // running is the scala one which does not implement consensus
              () -> Log.d(TAG, "subscription to consensus channel was a success"),
              error -> Log.d(TAG, "error while trying to subscribe to consensus channel"));
    }

    /* Creation channel coin*/
    context
        .getMessageSender()
        .subscribe(channel.subChannel("coin"))
        .subscribe(
            () -> Log.d(TAG, "subscription to the coin channel was a success"),
            error -> Log.d(TAG, "error while trying  to subscribe to coin channel"));

    laoRepository.updateNodes(channel);
  }

  /**
   * Process an UpdateLao message.
   *
   * @param context the HandlerContext of the message
   * @param updateLao the message that was received
   */
  public static void handleUpdateLao(HandlerContext context, UpdateLao updateLao)
      throws DataHandlingException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Log.d(TAG, " Receive Update Lao Broadcast msg=" + updateLao);
    Optional<LaoView> laoViewOptional = laoRepository.getLaoViewByChannel(channel);
    if (!laoViewOptional.isPresent()) {
      throw new DataHandlingException(updateLao, "Unknown LAO");
    }
    LaoView laoView = laoViewOptional.get();

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
      Log.d(TAG, "Cannot set the witness message title to update lao");
      throw new DataHandlingException(
          updateLao, "Cannot set the witness message title to update lao");
    }

    laoView.updateWitnessMessage(messageId, message);
    if (!laoView.isWitnessesEmpty()) {
      // We send a pending update only if there are already some witness that need to sign this
      // UpdateLao
      laoView.addPendingUpdate(new PendingUpdate(updateLao.getLastModified(), messageId));
    }
    laoRepository.updateNodes(channel);
    laoRepository.updateLao(laoView);
  }

  /**
   * Process a StateLao message.
   *
   * @param context the HandlerContext of the message
   * @param stateLao the message that was received
   */
  public static void handleStateLao(HandlerContext context, StateLao stateLao)
      throws DataHandlingException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();

    Log.d(TAG, "Receive State Lao Broadcast msg=" + stateLao);

    Optional<LaoView> laoViewOptional = laoRepository.getLaoViewByChannel(channel);
    if (!laoViewOptional.isPresent()) {
      throw new DataHandlingException(stateLao, "Unknown LAO");
    }
    LaoView laoView = laoViewOptional.get();

    Log.d(TAG, "Receive State Lao Broadcast " + stateLao.getName());
    if (!laoRepository.getMessageById().containsKey(stateLao.getModificationId())) {
      Log.d(TAG, "Can't find modification id : " + stateLao.getModificationId());
      // queue it if we haven't received the update message yet
      throw new InvalidMessageIdException(stateLao, stateLao.getModificationId());
    }

    Log.d(TAG, "Verifying signatures");
    for (PublicKeySignaturePair pair : stateLao.getModificationSignatures()) {
      if (!pair.getWitness().verify(pair.getSignature(), stateLao.getModificationId())) {
        throw new InvalidSignatureException(stateLao, pair.getSignature());
      }
    }

    Log.d(TAG, "Success to verify state lao signatures");

    // TODO: verify if lao/state_lao is consistent with the lao/update message

    laoView.updateLaoState(stateLao);

    PublicKey publicKey = context.getKeyManager().getMainPublicKey();
    if (laoView.isOrganizer(publicKey) || laoView.isWitness(publicKey)) {
      context
          .getMessageSender()
          .subscribe(laoView.getChannel().subChannel("consensus"))
          .subscribe();
    }

    // Now we're going to remove all pending updates which came prior to this state lao
    long targetTime = stateLao.getLastModified();
    laoView.removePendingUpdate(targetTime);

    laoRepository.updateLao(laoView);
    laoRepository.updateNodes(channel);
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

  public static void handleGreetLao(HandlerContext context, GreetLao greetLao)
      throws DataHandlingException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();

    Log.d(TAG, "handleGreetLao: channel " + channel + ", msg=" + greetLao);
    Optional<LaoView> laoViewOptional = laoRepository.getLaoViewByChannel(channel);
    if (!laoViewOptional.isPresent()) {
      throw new DataHandlingException(greetLao, "Unknown LAO");
    }
    LaoView laoView = laoViewOptional.get();

    // Check the correctness of the LAO id
    if (!laoView.getId().equals(greetLao.getId())) {
      Log.d(
          TAG,
          "Current lao id "
              + laoView.getId()
              + " doesn't match the lao id from greetLao message ("
              + greetLao.getId()
              + ")");
      throw new IllegalArgumentException(
          "Current lao doesn't match the lao id from the greetLao message");
    }
    Log.d(TAG, "Creating a server with IP: " + greetLao.getAddress());

    Server server = new Server(greetLao.getAddress(), greetLao.getFrontendKey());

    Log.d(TAG, "Adding the server to the repository for lao id : " + laoView.getId());
    ServerRepository serverRepository = context.getServerRepository();
    serverRepository.addServer(greetLao.getId(), server);

    // In the future, implement automatic connection to all the peers contained in the peers
    // message
  }
}
