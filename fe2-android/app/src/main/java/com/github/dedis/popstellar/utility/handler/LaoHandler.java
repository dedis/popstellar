package com.github.dedis.popstellar.utility.handler;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.PendingUpdate;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.security.Signature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Lao messages handler class */
public class LaoHandler {

  public static final String TAG = LaoHandler.class.getSimpleName();

  private LaoHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Process a LAO message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   * @param data the data of the message received
   * @param messageId the ID of the message received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleLaoMessage(
      LAORepository laoRepository, String channel, Data data, String messageId) {
    Log.d(TAG, "handle LAO message");

    switch (Objects.requireNonNull(Action.find(data.getAction()))) {
      case CREATE:
        return handleCreateLao(laoRepository, channel, (CreateLao) data);
      case UPDATE:
        return handleUpdateLao(laoRepository, channel, messageId, (UpdateLao) data);
      case STATE:
        return handleStateLao(laoRepository, channel, (StateLao) data);
      default:
        return true;
    }
  }

  /**
   * Process a CreateLao message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   * @param createLao the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleCreateLao(
      LAORepository laoRepository, String channel, CreateLao createLao) {
    Log.d(TAG, "handleCreateLao: channel " + channel + ", msg=" + createLao);
    Lao lao = laoRepository.getLaoByChannel(channel);

    lao.setName(createLao.getName());
    lao.setCreation(createLao.getCreation());
    lao.setLastModified(createLao.getCreation());
    lao.setOrganizer(createLao.getOrganizer());
    lao.setId(createLao.getId());
    lao.setWitnesses(new HashSet<>(createLao.getWitnesses()));

    return false;
  }

  /**
   * Process an UpdateLao message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   * @param messageId the ID of the received message
   * @param updateLao the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleUpdateLao(
      LAORepository laoRepository, String channel, String messageId, UpdateLao updateLao) {
    Log.d(TAG, " Receive Update Lao Broadcast msg=" + updateLao);
    Lao lao = laoRepository.getLaoByChannel(channel);

    if (lao.getLastModified() > updateLao.getLastModified()) {
      // the current state we have is more up to date
      return false;
    }

    WitnessMessage message;
    if (!updateLao.getName().equals(lao.getName())) {
      message = updateLaoNameWitnessMessage(messageId, updateLao, lao);
    } else if (!updateLao.getWitnesses().equals(lao.getWitnesses())) {
      message = updateLaoWitnessesWitnessMessage(messageId, updateLao, lao);
    } else {
      Log.d(TAG, " Cannot set the witness message title to update lao");
      return true;
    }

    lao.updateWitnessMessage(messageId, message);
    if (!lao.getWitnesses().isEmpty()) {
      // We send a pending update only if there are already some witness that need to sign this
      // UpdateLao
      lao.getPendingUpdates().add(new PendingUpdate(updateLao.getLastModified(), messageId));
    }
    return false;
  }

  /**
   * Process a StateLao message.
   *
   * @param laoRepository the repository to access the messages and LAO of the channel
   * @param channel the channel on which the message was received
   * @param stateLao the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleStateLao(
      LAORepository laoRepository, String channel, StateLao stateLao) {
    Log.d(TAG, "Receive State Lao Broadcast msg=" + stateLao);

    Lao lao = laoRepository.getLaoByChannel(channel);

    Log.d(TAG, "Receive State Lao Broadcast " + stateLao.getName());
    if (!laoRepository.getMessageById().containsKey(stateLao.getModificationId())) {
      Log.d(TAG, "Can't find modification id : " + stateLao.getModificationId());
      // queue it if we haven't received the update message yet
      return true;
    }

    Log.d(TAG, "Verifying signatures");
    // Verify signatures
    for (PublicKeySignaturePair pair : stateLao.getModificationSignatures()) {
      if (!Signature.verifySignature(
          stateLao.getModificationId(), pair.getWitness(), pair.getSignature())) {
        return false;
      }
    }
    Log.d(TAG, "Success to verify state lao signatures");

    // TODO: verify if lao/state_lao is consistent with the lao/update message

    lao.setId(stateLao.getId());
    lao.setWitnesses(stateLao.getWitnesses());
    lao.setName(stateLao.getName());
    lao.setLastModified(stateLao.getLastModified());
    lao.setModificationId(stateLao.getModificationId());

    // Now we're going to remove all pending updates which came prior to this state lao
    long targetTime = stateLao.getLastModified();
    lao.getPendingUpdates()
        .removeIf(pendingUpdate -> pendingUpdate.getModificationTime() <= targetTime);

    return false;
  }

  public static WitnessMessage updateLaoNameWitnessMessage(
      String messageId, UpdateLao updateLao, Lao lao) {
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
      String messageId, UpdateLao updateLao, Lao lao) {
    WitnessMessage message = new WitnessMessage(messageId);
    List<String> tempList = new ArrayList<>(updateLao.getWitnesses());
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
}
