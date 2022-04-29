package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Input;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Output;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.InputObject;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.OutputObject;
import com.github.dedis.popstellar.model.objects.ScriptInputObject;
import com.github.dedis.popstellar.model.objects.ScriptOutputObject;
import com.github.dedis.popstellar.model.objects.TransactionObject;
import com.github.dedis.popstellar.repository.LAORepository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TransactionCoinHandler {
  public static final String TAG = TransactionCoinHandler.class.getSimpleName();

  private TransactionCoinHandler() {
    throw new IllegalArgumentException("Utility class");
  }

  /**
   * Process an PostTransactionCoin.
   *
   * @param context the HandlerContext of the message
   * @param postTransactionCoin the data of the message that was received
   */
  public static void handlePostTransactionCoin(
      HandlerContext context, PostTransactionCoin postTransactionCoin) {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    Lao lao = laoRepository.getLaoByChannel(channel);

    TransactionObject transaction_object = new TransactionObject();

    transaction_object.setChannel(channel);
    transaction_object.setLockTime(postTransactionCoin.getTransaction().getLockTime());
    transaction_object.setVersion(postTransactionCoin.getTransaction().getVersion());

    // inputs and outputs for the creation
    List<InputObject> inputs = new ArrayList<>();
    List<OutputObject> outputs = new ArrayList<>();

    // Should always be at least one input and one output
    if (postTransactionCoin.getTransaction().getInputs().size() == 0
        || postTransactionCoin.getTransaction().getOutputs().size() == 0) {
      throw new IllegalArgumentException();
    }

    // Iterate on the inputs and the outputs
    Iterator<Input> iterator_input = postTransactionCoin.getTransaction().getInputs().iterator();
    Iterator<Output> iterator_output = postTransactionCoin.getTransaction().getOutputs().iterator();

    while (iterator_input.hasNext()) {
      Input current = iterator_input.next();
      // Normally if there is an input there is always a script
      if (current.getScript() == null) {
        throw new IllegalArgumentException();
      }
      ScriptInputObject script_ =
          new ScriptInputObject(
              current.getScript().getType(),
              current.getScript().getPubkey(),
              current.getScript().getSig());

      inputs.add(new InputObject(current.getTxOutHash(), current.getTxOutIndex(), script_));
    }

    while (iterator_output.hasNext()) {
      //Normally if there is an output there is always a script
      Output current = iterator_output.next();
      if (current.getScript() == null) {
        throw new IllegalArgumentException();
      }
      ScriptOutputObject script =
          new ScriptOutputObject(
              current.getScript().getType(), current.getScript().getPubkeyHash());
      outputs.add(new OutputObject(current.getValue(), script));
    }

    transaction_object.setInputs(inputs);
    transaction_object.setOutputs(outputs);

    // lao update the history / lao update the last transaction per public key
    lao.updateTransactionMaps(transaction_object);
  }
}
