import { ActionType, MessageRegistry, ObjectType } from 'core/network/jsonrpc/messages';

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
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configureNetwork(registry: MessageRegistry) {
  registry.add(
    ObjectType.ROLL_CALL,
    ActionType.CREATE,
    handleRollCallCreateMessage,
    CreateRollCall.fromJson,
  );
  registry.add(
    ObjectType.ROLL_CALL,
    ActionType.OPEN,
    handleRollCallOpenMessage,
    OpenRollCall.fromJson,
  );
  registry.add(
    ObjectType.ROLL_CALL,
    ActionType.CLOSE,
    handleRollCallCloseMessage,
    CloseRollCall.fromJson,
  );
  registry.add(
    ObjectType.ROLL_CALL,
    ActionType.REOPEN,
    handleRollCallReopenMessage,
    ReopenRollCall.fromJson,
  );
}
