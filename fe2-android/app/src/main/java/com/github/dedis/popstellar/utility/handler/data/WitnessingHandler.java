package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.*;

import javax.inject.Inject;

import timber.log.Timber;

public class WitnessingHandler {
  public static final String TAG = WitnessingHandler.class.getSimpleName();
  private final LAORepository laoRepo;

  @Inject
  public WitnessingHandler(LAORepository laoRepo) {
    this.laoRepo = laoRepo;
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
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    PublicKey witnessPk = context.getSenderPk();

    // Check that the sender of the message is a witness
    if (!laoView.getWitnesses().contains(witnessPk)) {
      throw new InvalidWitnessException(witnessMessageSignature, witnessPk);
    }

    // Check that a witness message with the given message id exists in the lao
    MessageID messageID = witnessMessageSignature.getMessageId();
    WitnessMessage witnessMessage = laoView.getWitnessMessages().get(messageID);
    if (witnessMessage == null) {
      throw new InvalidWitnessMessageException(witnessMessageSignature, messageID);
    }

    // Check that the signature of the message id is correct
    Signature signature = witnessMessageSignature.getSignature();
    if (!witnessPk.verify(signature, messageID)) {
      throw new InvalidSignatureException(witnessMessageSignature, signature);
    }

    witnessMessage.addWitness(witnessPk);
    Lao lao = laoView.createLaoCopy();
    lao.updateWitnessMessage(messageID, witnessMessage);

    laoRepo.updateLao(lao);
  }
}
