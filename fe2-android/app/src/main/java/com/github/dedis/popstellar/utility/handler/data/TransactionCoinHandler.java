package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.digitalcash.*;
import com.github.dedis.popstellar.repository.DigitalCashRepository;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import javax.inject.Inject;

public class TransactionCoinHandler {
  private static final Logger logger = LogManager.getLogger(TransactionCoinHandler.class);
  private final DigitalCashRepository digitalCashRepo;

  @Inject
  public TransactionCoinHandler(DigitalCashRepository digitalCashRepo) {
    this.digitalCashRepo = digitalCashRepo;
  }

  /**
   * Process an PostTransactionCoin.
   *
   * @param context the HandlerContext of the message
   * @param postTransactionCoin the data of the message that was received
   */
  public void handlePostTransactionCoin(
      HandlerContext context, PostTransactionCoin postTransactionCoin) throws NoRollCallException {
    Channel channel = context.getChannel();

    logger.debug("handlePostTransactionCoin: " + channel + " msg=" + postTransactionCoin);

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
    TransactionObjectBuilder builder =
        new TransactionObjectBuilder()
            .setChannel(channel)
            .setLockTime(postTransactionCoin.getTransaction().getLockTime())
            .setVersion(postTransactionCoin.getTransaction().getVersion())
            .setTransactionId(postTransactionCoin.getTransactionId())
            .setInputs(inputs)
            .setOutputs(outputs);

    digitalCashRepo.updateTransactions(channel.extractLaoId(), builder.build());
  }
}
