package com.github.dedis.popstellar.utility.handler;

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

/**
 * General message handler class
 */
public class GeneralHandler {

  private static final String TAG = GeneralHandler.class.getSimpleName();

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
    Log.d(TAG, "handle incoming message");

    // Put the message in the state
    laoRepository.getMessageById().put(message.getMessageId(), message);

    String senderPk = message.getSender();

    Data data = message.getData();
    Log.d(TAG, "data with class: " + data.getClass());
    boolean enqueue = false;
    if (data instanceof CreateLao) {
      enqueue = LaoHandler.handleCreateLao(laoRepository, channel, (CreateLao) data);
    } else if (data instanceof UpdateLao) {
      enqueue = LaoHandler
          .handleUpdateLao(laoRepository, channel, message.getMessageId(), (UpdateLao) data);
    } else if (data instanceof StateLao) {
      enqueue = LaoHandler.handleStateLao(laoRepository, channel, (StateLao) data);
    } else if (data instanceof CreateRollCall) {
      enqueue = RollCallHandler
          .handleCreateRollCall(laoRepository, channel, (CreateRollCall) data,
              message.getMessageId());
    } else if (data instanceof OpenRollCall) {
      enqueue = RollCallHandler
          .handleOpenRollCall(laoRepository, channel, (OpenRollCall) data, message.getMessageId());
    } else if (data instanceof CloseRollCall) {
      enqueue = RollCallHandler
          .handleCloseRollCall(laoRepository, channel, (CloseRollCall) data,
              message.getMessageId());
    } else if (data instanceof ElectionSetup) {
      enqueue = ElectionHandler
          .handleElectionSetup(laoRepository, channel, (ElectionSetup) data,
              message.getMessageId());
    } else if (data instanceof ElectionResult) {
      enqueue = ElectionHandler.handleElectionResult(laoRepository, channel, (ElectionResult) data);
    } else if (data instanceof ElectionEnd) {
      enqueue = ElectionHandler.handleElectionEnd(laoRepository, channel);
    } else if (data instanceof CastVote) {
      enqueue = ElectionHandler
          .handleCastVote(laoRepository, channel, (CastVote) data, senderPk,
              message.getMessageId());
    } else if (data instanceof WitnessMessageSignature) {
      enqueue = WitnessMessageHandler
          .handleWitnessMessage(laoRepository, channel, senderPk, (WitnessMessageSignature) data);
    } else {
      Log.d(TAG, "cannot handle message with data" + data.getClass());
      enqueue = true;
    }

    // Trigger an onNext
    if (!(data instanceof WitnessMessageSignature) && laoRepository.isLaoChannel(channel)) {
      LAOState laoState = laoRepository.getLaoById().get(channel);
      laoState.publish();
      if (data instanceof StateLao || data instanceof CreateLao) {
        laoRepository.setAllLaoSubject();
      }
    }
    return enqueue;
  }
}
