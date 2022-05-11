import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';

import { RollCallConfiguration } from '../interface';
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
  configuration.messageRegistry.add(
    ObjectType.ROLL_CALL,
    ActionType.CREATE,
    handleRollCallCreateMessage(configuration.addEvent),
    CreateRollCall.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ROLL_CALL,
    ActionType.OPEN,
    handleRollCallOpenMessage(configuration.getEventById, configuration.updateEvent),
    OpenRollCall.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ROLL_CALL,
    ActionType.CLOSE,
    handleRollCallCloseMessage(
      configuration.getEventById,
      configuration.updateEvent,
      configuration.generateToken,
      configuration.setLaoLastRollCall,
    ),
    CloseRollCall.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.ROLL_CALL,
    ActionType.REOPEN,
    handleRollCallReopenMessage(configuration.getEventById, configuration.updateEvent),
    ReopenRollCall.fromJson,
  );
};
