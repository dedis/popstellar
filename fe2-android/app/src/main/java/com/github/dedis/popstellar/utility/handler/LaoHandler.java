package com.github.dedis.popstellar.utility.handler;

import android.util.Log;
import com.github.dedis.popstellar.model.Lao;
import com.github.dedis.popstellar.model.PendingUpdate;
import com.github.dedis.popstellar.model.WitnessMessage;
import com.github.dedis.popstellar.model.data.LAORepository;
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.utility.security.Signature;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Lao messages handler class
 */
public class LaoHandler {

  private static final String TAG = LaoHandler.class.getSimpleName();

  private static final String LAO_NAME = " Lao Name : ";
  private static final String OLD_NAME = " Old Name : ";
  private static final String NEW_NAME = " New Name : ";
  private static final String MESSAGE_ID = "Message ID : ";
  private static final String UPDATE_LAO = "Update Lao Name ";
  private static final String WITNESS_ID = " New Witness ID : ";
  private static final String UPDATE_WITNESS = "Update Lao Witnesses  ";

  /**
   * Process a CreateLao message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel       the channel on which the message was received
   * @param createLao     the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleCreateLao(LAORepository laoRepository, String channel,
      CreateLao createLao) {
    Log.d(TAG, "handleCreateLao: channel " + channel + "LAO name " + createLao.getName());
    Lao lao = laoRepository.getLaoByChannel(channel);

    lao.setName(createLao.getName());
    lao.setCreation(createLao.getCreation());
    lao.setLastModified(createLao.getCreation());
    lao.setOrganizer(createLao.getOrganizer());
    lao.setId(createLao.getId());
    lao.setWitnesses(new HashSet<>(createLao.getWitnesses()));

    Log.d(
        TAG,
        "Setting name as "
            + createLao.getName()
            + " creation time as "
            + createLao.getCreation()
            + " lao channel is "
            + channel);

    return false;
  }

  /**
   * Process a UpdateLao message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel       the channel on which the message was received
   * @param messageId     the ID of the received message
   * @param updateLao     the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleUpdateLao(LAORepository laoRepository, String channel,
      String messageId, UpdateLao updateLao) {
    Log.d(TAG, " Receive Update Lao Broadcast");
    Lao lao = laoRepository.getLaoByChannel(channel);

    if (lao.getLastModified() > updateLao.getLastModified()) {
      // the current state we have is more up to date
      return false;
    }

    WitnessMessage message = new WitnessMessage(messageId);
    if (!updateLao.getName().equals(lao.getName())) {
      message.setTitle(UPDATE_LAO);
      message.setDescription(
          OLD_NAME + lao.getName() + "\n" + NEW_NAME + updateLao.getName() +
              "\n" + MESSAGE_ID + messageId);
    } else if (!updateLao.getWitnesses().equals(lao.getWitnesses())) {
      List<String> tempList = new ArrayList<>(updateLao.getWitnesses());
      message.setTitle(UPDATE_WITNESS);
      message.setDescription(LAO_NAME + lao.getName() + "\n" + MESSAGE_ID + messageId + "\n"
          + WITNESS_ID + tempList.get(tempList.size() - 1)
      );

    } else {
      Log.d(TAG, " Problem to set the witness message title for update lao");
    }

    lao.updateWitnessMessage(messageId, message);
    if (!lao.getWitnesses().isEmpty()) {
      // We send a pending update only if there are already some witness that need to sign this UpdateLao
      lao.getPendingUpdates().add(new PendingUpdate(updateLao.getLastModified(), messageId));
    }
    return false;
  }

  /**
   * Process a StateLao message.
   *
   * @param laoRepository the repository to access the messages and LAO of the channel
   * @param channel       the channel on which the message was received
   * @param stateLao      the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleStateLao(LAORepository laoRepository, String channel,
      StateLao stateLao) {
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
      if (!Signature
          .verifySignature(stateLao.getModificationId(), pair.getWitness(), pair.getSignature())) {
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
}
