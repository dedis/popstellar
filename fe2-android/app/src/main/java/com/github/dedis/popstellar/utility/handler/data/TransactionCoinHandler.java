package com.github.dedis.popstellar.utility.handler.data;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.digitalcash.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.*;

import javax.inject.Inject;

public class TransactionCoinHandler {
  public static final String TAG = TransactionCoinHandler.class.getSimpleName();
  private final LAORepository laoRepo;

  @Inject
  public TransactionCoinHandler(LAORepository laoRepo) {
    this.laoRepo = laoRepo;
  }

  /**
   * Process an PostTransactionCoin.
   *
   * @param context the HandlerContext of the message
   * @param postTransactionCoin the data of the message that was received
   */
  public void handlePostTransactionCoin(
      HandlerContext context, PostTransactionCoin postTransactionCoin) throws UnknownLaoException {
    Channel channel = context.getChannel();

    Log.d(TAG, "handlePostTransactionCoin: " + channel + " msg=" + postTransactionCoin);

    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    TransactionObjectBuilder builder = new TransactionObjectBuilder();

    // inputs and outputs for the creation
    List<InputObject> inputs = new ArrayList<>();
    List<OutputObject> outputs = new ArrayList<>();

    // Should always be at least one input and one output
    if (postTransactionCoin.getTransaction().getInputs().isEmpty()
        || postTransactionCoin.getTransaction().getOutputs().isEmpty()) {
      throw new IllegalArgumentException();
    }

    // Iterate on the inputs and the outputs
    Iterator<Input> iteratorInput = postTransactionCoin.getTransaction().getInputs().iterator();
    Iterator<Output> iteratorOutput = postTransactionCoin.getTransaction().getOutputs().iterator();

    while (iteratorInput.hasNext()) {
      Input current = iteratorInput.next();
      // Normally if there is an input there is always a script
      if (current.getScript() == null) {
        throw new IllegalArgumentException();
      }
      ScriptInputObject scriptInputObject =
          new ScriptInputObject(
              current.getScript().getType(),
              current.getScript().getPubkey(),
              current.getScript().getSig());

      inputs.add(
          new InputObject(current.getTxOutHash(), current.getTxOutIndex(), scriptInputObject));
    }

    while (iteratorOutput.hasNext()) {
      // Normally if there is an output there is always a script
      Output current = iteratorOutput.next();
      if (current.getScript() == null) {
        throw new IllegalArgumentException();
      }
      ScriptOutputObject script =
          new ScriptOutputObject(
              current.getScript().getType(), current.getScript().getPubKeyHash());
      outputs.add(new OutputObject(current.getValue(), script));
    }
    builder
        .setChannel(channel)
        .setLockTime(postTransactionCoin.getTransaction().getLockTime())
        .setVersion(postTransactionCoin.getTransaction().getVersion())
        .setTransactionId(postTransactionCoin.getTransactionId())
        .setInputs(inputs)
        .setOutputs(outputs);

    Lao lao = laoView.createLaoCopy();

    // lao update the history / lao update the last transaction per public key
    lao.updateTransactionMaps(builder.build());
    laoRepo.updateLao(lao);
  }
}
