package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.repository.LAORepository;

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
      HandlerContext context, WitnessMessageSignature witnessMessageSignature) {
    Timber.tag(TAG).d("WitnessMessageSignature handler not implemented");
  }
}
