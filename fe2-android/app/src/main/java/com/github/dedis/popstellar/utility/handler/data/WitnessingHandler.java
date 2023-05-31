package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.WitnessingRepository;
import com.github.dedis.popstellar.utility.error.*;

import javax.inject.Inject;

import timber.log.Timber;

public class WitnessingHandler {
  public static final String TAG = WitnessingHandler.class.getSimpleName();
  private final LAORepository laoRepo;
  private final WitnessingRepository witnessingRepo;

  @Inject
  public WitnessingHandler(LAORepository laoRepo, WitnessingRepository witnessingRepo) {
    this.laoRepo = laoRepo;
    this.witnessingRepo = witnessingRepo;
  }

  /**
   * Process a WitnessMessageSignature message.
   *
   * @param context the HandlerContext of the message
   * @param witnessMessageSignature the message that was received
   */
  public void handleWitnessMessageSignature(
      HandlerContext context, WitnessMessageSignature witnessMessageSignature)
      throws UnknownLaoException, DataHandlingException {
    Timber.tag(TAG)
        .d("Received a WitnessMessageSignature Broadcast msg: %s", witnessMessageSignature);
    Channel channel = context.getChannel();
    String laoId = laoRepo.getLaoViewByChannel(channel).getId();
    PublicKey witnessPk = context.getSenderPk();

    // Check that the sender of the message is a witness
    if (!witnessingRepo.isWitness(laoId, witnessPk)) {
      throw new InvalidWitnessingException(witnessMessageSignature, witnessPk);
    }

    // Check that the signature of the message id is correct
    MessageID messageID = witnessMessageSignature.getMessageId();
    Signature signature = witnessMessageSignature.getSignature();
    if (!witnessPk.verify(signature, messageID)) {
      throw new InvalidSignatureException(witnessMessageSignature, signature);
    }

    // Add the witness to the witness message in the repository
    if (!witnessingRepo.addWitnessToMessage(laoId, messageID, witnessPk)) {
      throw new InvalidWitnessingException(witnessMessageSignature, messageID);
    }
  }
}
