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
   */
  default void handle(CreateLao createLao) {}

  /**
   * Handle a StateLao data message
   *
   * @param stateLao to handle
   */
  default void handle(StateLao stateLao) {}

  /**
   * Handle a UpdateLao data message
   *
   * @param updateLao to handle
   */
  default void handle(UpdateLao updateLao) {}

  /**
   * Handle a CreateMeeting data message
   *
   * @param createMeeting to handle
   */
  default void handle(CreateMeeting createMeeting) {}

  /**
   * Handle a StateMeeting data message
   *
   * @param stateMeeting to handle
   */
  default void handle(StateMeeting stateMeeting) {}

  /**
   * Handle a WitnessMessage data message
   *
   * @param witnessMessage to handle
   */
  default void handle(WitnessMessage witnessMessage) {}

  /**
   * Handle a CloseRollCall data message
   *
   * @param closeRollCall to handle
   */
  default void handle(CloseRollCall closeRollCall) {}

  /**
   * Handle a CreateRollCall data message
   *
   * @param createRollCall to handle
   */
  default void handle(CreateRollCall createRollCall) {}

  /**
   * Handle a OpenRollCall data message
   *
   * @param openRollCall to handle
   */
  default void handle(OpenRollCall openRollCall) {}
}
