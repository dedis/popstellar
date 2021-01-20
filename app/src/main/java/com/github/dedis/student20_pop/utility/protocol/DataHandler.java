package com.github.dedis.student20_pop.utility.protocol;

import com.github.dedis.student20_pop.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.StateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.student20_pop.model.network.method.message.data.message.WitnessMessage;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.OpenRollCall;

import java.net.URI;

/**
 * Handler of the high-level messages, called Data
 *
 * @see com.github.dedis.student20_pop.model.network.method.message.data.Data
 */
public interface DataHandler {

  /**
   * Handle a CreateLao data message
   *
   * @param createLao to handle
   * @param host the message was received from
   * @param channel on which the message was received
   */
  default void handle(CreateLao createLao, URI host, String channel) {
    throw new UnsupportedOperationException();
  }

  /**
   * Handle a StateLao data message
   *  @param stateLao to handle
   * @param host the message was received from
   * @param channel on which the message was received
   */
  default void handle(StateLao stateLao, URI host, String channel) {
    throw new UnsupportedOperationException();
  }

  /**
   * Handle a UpdateLao data message
   *  @param updateLao to handle
   * @param host the message was received from
   * @param channel on which the message was received
   */
  default void handle(UpdateLao updateLao, URI host, String channel) {
    throw new UnsupportedOperationException();
  }

  /**
   * Handle a CreateMeeting data message
   *  @param createMeeting to handle
   * @param host the message was received from
   * @param channel on which the message was received
   */
  default void handle(CreateMeeting createMeeting, URI host, String channel) {
    throw new UnsupportedOperationException();
  }

  /**
   * Handle a StateMeeting data message
   *
   * @param stateMeeting to handle
   * @param host the message was received from
   * @param channel on which the message was received
   */
  default void handle(StateMeeting stateMeeting, URI host, String channel) {
    throw new UnsupportedOperationException();
  }

  /**
   * Handle a WitnessMessage data message
   *  @param witnessMessage to handle
   * @param host the message was received from
   * @param channel on which the message was received
   */
  default void handle(WitnessMessage witnessMessage, URI host, String channel) {
    throw new UnsupportedOperationException();
  }

  /**
   * Handle a CloseRollCall data message
   *  @param closeRollCall to handle
   * @param host the message was received from
   * @param channel on which the message was received
   */
  default void handle(CloseRollCall closeRollCall, URI host, String channel) {
    throw new UnsupportedOperationException();
  }

  /**
   * Handle a CreateRollCall data message
   *  @param createRollCall to handle
   * @param host the message was received from
   * @param channel on which the message was received
   */
  default void handle(CreateRollCall createRollCall, URI host, String channel) {
    throw new UnsupportedOperationException();
  }

  /**
   * Handle a OpenRollCall data message
   *  @param openRollCall to handle
   * @param host the message was received from
   * @param channel on which the message was received
   */
  default void handle(OpenRollCall openRollCall, URI host, String channel) {
    throw new UnsupportedOperationException();
  }
}
