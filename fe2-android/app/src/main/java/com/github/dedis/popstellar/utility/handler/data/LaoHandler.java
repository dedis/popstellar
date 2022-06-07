package com.github.dedis.popstellar.utility.handler.data;

import android.annotation.SuppressLint;
import android.util.Log;
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.GreetLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.PendingUpdate;
import com.github.dedis.popstellar.model.objects.Server;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.ServerRepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;
import com.github.dedis.popstellar.utility.error.InvalidSignatureException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
    Channel channel = context.getChannel();

    Log.d(TAG, "handleCreateLao: channel " + channel + ", msg=" + createLao);
    Lao lao = new Lao(createLao.getId());

    // Adding the newly created LAO to the repository
    laoRepository.getLaoById().put(lao.getId(), new LAOState(lao));
    laoRepository.setAllLaoSubject();

    lao.setName(createLao.getName());
    lao.setCreation(createLao.getCreation());
    lao.setLastModified(createLao.getCreation());
    lao.setOrganizer(createLao.getOrganizer());
    lao.setId(createLao.getId());
    lao.setWitnesses(new HashSet<>(createLao.getWitnesses()));

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
    Lao lao = laoRepository.getLaoByChannel(channel);

    if (lao.getLastModified() > updateLao.getLastModified()) {
      // the current state we have is more up to date
      throw new DataHandlingException(
          updateLao, "The current Lao is more up to date than the update lao message");
    }

    WitnessMessage message;
    if (!updateLao.getName().equals(lao.getName())) {
      message = updateLaoNameWitnessMessage(messageId, updateLao, lao);
    } else if (!updateLao.getWitnesses().equals(lao.getWitnesses())) {
      message = updateLaoWitnessesWitnessMessage(messageId, updateLao, lao);
    } else {
      Log.d(TAG, "Cannot set the witness message title to update lao");
      throw new DataHandlingException(
          updateLao, "Cannot set the witness message title to update lao");
    }

    lao.updateWitnessMessage(messageId, message);
    if (!lao.getWitnesses().isEmpty()) {
      // We send a pending update only if there are already some witness that need to sign this
      // UpdateLao
      lao.getPendingUpdates().add(new PendingUpdate(updateLao.getLastModified(), messageId));
    }
    laoRepository.updateNodes(channel);
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

    Lao lao = laoRepository.getLaoByChannel(channel);

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

    lao.setId(stateLao.getId());
    lao.setWitnesses(stateLao.getWitnesses());
    lao.setName(stateLao.getName());
    lao.setLastModified(stateLao.getLastModified());
    lao.setModificationId(stateLao.getModificationId());

    PublicKey publicKey = context.getKeyManager().getMainPublicKey();
    if (lao.getOrganizer().equals(publicKey) || lao.getWitnesses().contains(publicKey)) {
      context.getMessageSender().subscribe(lao.getChannel().subChannel("consensus")).subscribe();
    }

    // Now we're going to remove all pending updates which came prior to this state lao
    long targetTime = stateLao.getLastModified();
    lao.getPendingUpdates()
        .removeIf(pendingUpdate -> pendingUpdate.getModificationTime() <= targetTime);
    laoRepository.updateNodes(channel);
  }

  public static WitnessMessage updateLaoNameWitnessMessage(
      MessageID messageId, UpdateLao updateLao, Lao lao) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("Update Lao Name ");
    message.setDescription(
        "Old Name : "
            + lao.getName()
            + "\n"
            + "New Name : "
            + updateLao.getName()
            + "\n"
            + "Message ID : "
            + messageId);
    return message;
  }

  public static WitnessMessage updateLaoWitnessesWitnessMessage(
      MessageID messageId, UpdateLao updateLao, Lao lao) {
    WitnessMessage message = new WitnessMessage(messageId);
    List<PublicKey> tempList = new ArrayList<>(updateLao.getWitnesses());
    message.setTitle("Update Lao Witnesses");
    message.setDescription(
        "Lao Name : "
            + lao.getName()
            + "\n"
            + "Message ID : "
            + messageId
            + "\n"
            + "New Witness ID : "
            + tempList.get(tempList.size() - 1));
    return message;
  }

  public static void handleGreetLao(HandlerContext context, GreetLao greetLao) {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();

    Log.d(TAG, "handleGreetLao: channel " + channel + ", msg=" + greetLao);
    Lao lao = laoRepository.getLaoByChannel(channel);

    // Check the correctness of the LAO id
    if (!lao.getId().equals(greetLao.getId())) {
      Log.d(
          TAG,
          "Current lao id "
              + lao.getId()
              + " doesn't match the lao id from greetLao message ("
              + greetLao.getId()
              + ")");
      throw new IllegalArgumentException(
          "Current lao doesn't march the lao id from the greetLao message");
    }
    Log.d(TAG, "Creating a server with IP: " + greetLao.getAddress());

    Server server = new Server(greetLao.getAddress(), greetLao.getFrontendKey());

    Log.d(TAG, "Adding the server to the repository for lao id : " + lao.getId());
    ServerRepository serverRepository = context.getServerRepository();
    serverRepository.addServer(greetLao.getId(), server);

    // In the future, implement automatic connection to all the peers contained in the peers
    // message
  }
}
