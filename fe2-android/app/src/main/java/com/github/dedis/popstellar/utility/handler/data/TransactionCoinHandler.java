package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Input;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Output;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Input_object;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.Output_object;
import com.github.dedis.popstellar.model.objects.Script_input_object;
import com.github.dedis.popstellar.model.objects.Script_output_object;
import com.github.dedis.popstellar.model.objects.Transaction_object;
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

    Transaction_object transaction_object = new Transaction_object();

    transaction_object.setChannel(channel);
    transaction_object.setLock_time(postTransactionCoin.get_transaction().get_lock_time());
    transaction_object.setVersion(postTransactionCoin.get_transaction().get_version());

    //inputs and outputs for the creation
    List<Input_object> inputs = new ArrayList<>();
    List<Output_object> outputs = new ArrayList<>();

    //Should always be at least one input and one output
    if ( postTransactionCoin.get_transaction().get_inputs().size() == 0 ||
            postTransactionCoin.get_transaction().get_outputs().size() == 0
    ) {
      throw new IllegalArgumentException();
    }

    // Iterate on the inputs and the outputs
    Iterator<Input> iterator_input = postTransactionCoin.get_transaction().get_inputs().iterator();
    Iterator<Output> iterator_output =
        postTransactionCoin.get_transaction().get_outputs().iterator();

    while (iterator_input.hasNext()) {
      Input current = iterator_input.next();
      //Normally if there is an input there is always a script
      if (current.get_script() == null){
        throw new IllegalArgumentException();
      }
      Script_input_object script_ =
          new Script_input_object(
              current.get_script().get_type(),
              current.get_script().get_pubkey(),
              current.get_script().get_sig());

      inputs.add(new Input_object(current.get_tx_out_hash(), current.get_tx_out_index(), script_));
    }

    while (iterator_output.hasNext()) {
      //Normally if there is an output there is always a script
      Output current = iterator_output.next();
      if (current.get_script() == null){
        throw new IllegalArgumentException();
      }
      Script_output_object script = new Script_output_object(
              current.get_script().get_type(),
              current.get_script().get_pubkey_hash()
      );
      outputs.add(new Output_object(current.get_value(),script));
    }

    transaction_object.setInputs(inputs);
    transaction_object.setOutputs(outputs);

    // lao update the history / lao update the last transaction per public key
    lao.updateTransactionMaps(transaction_object);
  }
}
