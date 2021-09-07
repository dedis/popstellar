package com.github.dedis.popstellar.model.data.handler;

import static com.github.dedis.popstellar.model.data.handler.ElectionHandler.handleCastVote;
import static com.github.dedis.popstellar.model.data.handler.ElectionHandler.handleElectionEnd;
import static com.github.dedis.popstellar.model.data.handler.ElectionHandler.handleElectionResult;
import static com.github.dedis.popstellar.model.data.handler.ElectionHandler.handleElectionSetup;
import static com.github.dedis.popstellar.model.data.handler.LaoHandler.handleCreateLao;
import static com.github.dedis.popstellar.model.data.handler.LaoHandler.handleStateLao;
import static com.github.dedis.popstellar.model.data.handler.LaoHandler.handleUpdateLao;
import static com.github.dedis.popstellar.model.data.handler.RollCallHandler.handleCloseRollCall;
import static com.github.dedis.popstellar.model.data.handler.RollCallHandler.handleCreateRollCall;
import static com.github.dedis.popstellar.model.data.handler.RollCallHandler.handleOpenRollCall;
import static com.github.dedis.popstellar.model.data.handler.WitnessMessageHandler.handleWitnessMessage;

import android.util.Log;
import com.github.dedis.popstellar.model.data.LAORepository;
import com.github.dedis.popstellar.model.data.LAOState;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import java.util.stream.Collectors;

/**
 * General message handler class
 */
public class GeneralHandler {

  public static final String TAG = GeneralHandler.class.getSimpleName();

  /**
   * Send messages to the corresponding handler.
   *
   * @param laoRepository the repository to access the messages and LAOs
   * @param channel       the channel on which the message was received
   * @param message       the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleMessage(LAORepository laoRepository, String channel,
      MessageGeneral message) {
    // TODO: move to default, do and then call handle message
    // Put the message in the state
    laoRepository.getMessageById().put(message.getMessageId(), message);

    String senderPk = message.getSender();

    Data data = message.getData();
    Log.d(TAG, "data with class: " + data.getClass());
    boolean enqueue = false;
    if (data instanceof CreateLao) {
      enqueue = handleCreateLao(laoRepository, channel, (CreateLao) data);
    } else if (data instanceof UpdateLao) {
      enqueue = handleUpdateLao(laoRepository, channel, message.getMessageId(), (UpdateLao) data);
    } else if (data instanceof StateLao) {
      enqueue = handleStateLao(laoRepository, channel, (StateLao) data);
    } else if (data instanceof CreateRollCall) {
      enqueue = handleCreateRollCall(laoRepository, channel, (CreateRollCall) data,
          message.getMessageId());
    } else if (data instanceof OpenRollCall) {
      enqueue = handleOpenRollCall(laoRepository, channel, (OpenRollCall) data,
          message.getMessageId());
    } else if (data instanceof CloseRollCall) {
      enqueue = handleCloseRollCall(laoRepository, channel, (CloseRollCall) data,
          message.getMessageId());
    } else if (data instanceof ElectionSetup) {
      enqueue = handleElectionSetup(laoRepository, channel, (ElectionSetup) data,
          message.getMessageId());
    } else if (data instanceof ElectionResult) {
      enqueue = handleElectionResult(laoRepository, channel, (ElectionResult) data);
    } else if (data instanceof ElectionEnd) {
      enqueue = handleElectionEnd(laoRepository, channel);
    } else if (data instanceof CastVote) {
      enqueue = handleCastVote(laoRepository, channel, (CastVote) data, senderPk,
          message.getMessageId());
    } else if (data instanceof WitnessMessageSignature) {
      enqueue = handleWitnessMessage(laoRepository, channel, senderPk,
          (WitnessMessageSignature) data);
    } else {
      Log.d(TAG, "cannot handle message with data" + data.getClass());
      enqueue = true;
    }

    // TODO: move to a first Handler class.
    // Trigger an onNext
    if (!(data instanceof WitnessMessageSignature) && laoRepository.isLaoChannel(channel)) {
      LAOState laoState = laoRepository.getLaoById().get(channel);
      laoState.publish();
      if (data instanceof StateLao || data instanceof CreateLao) {
        laoRepository.getAllLaoSubject().onNext(
            laoRepository.getLaoById().entrySet().stream()
                .map(x -> x.getValue().getLao())
                .collect(Collectors.toList()));
      }
    }
    return enqueue;
  }
}
