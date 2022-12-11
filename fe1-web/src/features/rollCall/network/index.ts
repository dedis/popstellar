import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import { dispatch, getStore } from 'core/redux';

import { RollCallConfiguration } from '../interface';
import { RollCall } from '../objects';
import { addRollCall, getRollCallById, updateRollCall } from '../reducer';
import { CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall } from './messages';
import {
  handleRollCallCloseMessage,
  handleRollCallCreateMessage,
  handleRollCallOpenMessage,
  handleRollCallReopenMessage,
} from './RollCallHandler';

export * from './RollCallMessageApi';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param configuration - The configuration object for the rollcall feature
 */
export const configureNetwork = (configuration: RollCallConfiguration) => {
  // getRollCallById bound to the global state
  const boundGetRollCallById = (rollCallId: Hash) =>
    getRollCallById(rollCallId, getStore().getState());

  const addRollCallEvent = (laoId: Hash, rollCall: RollCall) => {
    const rollCallState = rollCall.toState();

    dispatch(addRollCall(rollCallState));
    dispatch(
      configuration.addEvent(laoId, {
        eventType: RollCall.EVENT_TYPE,
        id: rollCallState.id,
        start: rollCall.start.valueOf(),
        end: rollCall.end?.valueOf(),
      }),
    );
  };

  const updateRollCallEvent = (rollCall: RollCall) => {
    const rollCallState = rollCall.toState();

    dispatch(updateRollCall(rollCallState));
    dispatch(
      configuration.updateEvent({
        eventType: RollCall.EVENT_TYPE,
        id: rollCallState.id,
        start: rollCall.start.valueOf(),
        end: rollCall.end?.valueOf(),
      }),
    );
  };

  configuration.messageRegistry.add(
    ObjectType.ROLL_CALL,
    ActionType.CREATE,
    handleRollCallCreateMessage(addRollCallEvent),
    CreateRollCall.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ROLL_CALL,
    ActionType.OPEN,
    handleRollCallOpenMessage(boundGetRollCallById, updateRollCallEvent),
    OpenRollCall.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ROLL_CALL,
    ActionType.CLOSE,
    handleRollCallCloseMessage(
      boundGetRollCallById,
      updateRollCallEvent,
      configuration.generateToken,
      configuration.setLaoLastRollCall,
    ),
    CloseRollCall.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ROLL_CALL,
    ActionType.REOPEN,
    handleRollCallReopenMessage(boundGetRollCallById, updateRollCallEvent),
    ReopenRollCall.fromJson,
  );
};
