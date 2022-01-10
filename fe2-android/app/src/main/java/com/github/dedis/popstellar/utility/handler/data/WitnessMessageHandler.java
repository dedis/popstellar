package com.github.dedis.popstellar.utility.handler.data;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.PendingUpdate;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.InvalidDataException;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;
import com.github.dedis.popstellar.utility.error.InvalidSignatureException;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Witness messages handler class */
public final class WitnessMessageHandler {

  private static final String TAG = WitnessMessage.class.getSimpleName();

  private WitnessMessageHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Process a WitnessMessageSignature message
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   * @param sender the public key of the sender
   * @param data the message that was received
   */
  public static void handleWitnessMessage(
      LAORepository laoRepository, String channel, PublicKey sender, Data data)
      throws DataHandlingException {
    WitnessMessageSignature message = (WitnessMessageSignature) data;

    Log.d(TAG, "Received Witness Message Signature Broadcast with id : " + message.getMessageId());
    MessageID messageId = message.getMessageId();
    com.github.dedis.popstellar.model.objects.security.Signature signature = message.getSignature();

    // Verify signature
    if (!sender.verify(signature, messageId)) {
      Log.w(
          TAG,
          "Failed to verify signature of Witness Message Signature id="
              + messageId
              + ", signature="
              + signature);
      throw new InvalidSignatureException(message, signature);
    }

    MessageGeneral msg = laoRepository.getMessageById().get(messageId);
    if (msg == null) throw new InvalidMessageIdException(message, messageId);

    // Update the message
    msg.getWitnessSignatures().add(new PublicKeySignaturePair(sender, signature));
    Log.d(TAG, "Message General updated with the new Witness Signature");

    Lao lao = laoRepository.getLaoByChannel(channel);
    if (lao == null) {
      Log.d(TAG, "failed to retrieve the lao with channel " + channel);
      throw new InvalidDataException(data, "lao's channel", channel);
    }
    // Update WitnessMessage of the corresponding lao
    updateWitnessMessage(lao, message, sender);
    Log.d(TAG, "WitnessMessage successfully updated");

    Set<PendingUpdate> pendingUpdates = lao.getPendingUpdates();
    // Check if any pending update contains messageId
    if (pendingUpdates.stream().anyMatch(ob -> ob.getMessageId().equals(messageId))) {
      Log.d(TAG, "There is a pending update for this message");

      // Let's check if we have enough signatures
      Set<PublicKey> signaturesCollectedSoFar =
          msg.getWitnessSignatures().stream()
              .map(PublicKeySignaturePair::getWitness)
              .collect(Collectors.toSet());
      if (lao.getWitnesses().equals(signaturesCollectedSoFar)) {
        Log.d(TAG, "We have enough signatures for the UpdateLao so we can send a StateLao");

        // We send a state lao if we are the organizer
        laoRepository.sendStateLao(lao, msg, messageId, channel);
      }
    }
  }

  /**
   * Helper method to update the WitnessMessage of the lao with the new witness signing
   *
   * @param message Base 64 URL encoded Id of the message to sign
   * @param senderPk Base 64 URL encoded public key of the signer
   * @throws DataHandlingException if an error occurs during the update
   */
  private static void updateWitnessMessage(
      Lao lao, WitnessMessageSignature message, PublicKey senderPk) throws DataHandlingException {
    MessageID messageId = message.getMessageId();
    Optional<WitnessMessage> optionalWitnessMessage = lao.getWitnessMessage(messageId);

    // We update the corresponding  witness message of the lao with a new witness that signed it.
    WitnessMessage witnessMessage =
        optionalWitnessMessage.orElseThrow(() -> new InvalidMessageIdException(message, messageId));
    witnessMessage.addWitness(senderPk);
    Log.d(TAG, "Updated the WitnessMessage with a new witness " + messageId);
    lao.updateWitnessMessage(messageId, witnessMessage);
  }
}
