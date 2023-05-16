package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

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

    if (!laoView.getWitnesses().contains(witnessPk)) {
      throw new DataHandlingException(
          witnessMessageSignature, "The sender of the WitnessMessageSignature is not a witness");
    }

    MessageID messageID = witnessMessageSignature.getMessageId();
    WitnessMessage witnessMessage = laoView.getWitnessMessages().get(messageID);
    if (witnessMessage == null) {
      throw new DataHandlingException(
          witnessMessageSignature,
          "No witness signature with message id " + messageID + " exists in the lao");
    }

    witnessMessage.addWitness(witnessPk);
    Lao lao = laoView.createLaoCopy();
    lao.updateWitnessMessage(messageID, witnessMessage);

    laoRepo.updateLao(lao);
  }
}
