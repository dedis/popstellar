import { dispatch } from 'core/redux';

import { Broadcast, JsonRpcMethod, ExtendedJsonRpcRequest } from '../jsonrpc';
import { ActionType, MessageRegistry, ObjectType } from '../jsonrpc/messages';
import { ExtendedMessage } from './ExtendedMessage';
import { addMessages } from './MessageReducer';

let messageRegistry: MessageRegistry;

/**
 * Dependency injection of a MessageRegistry to avoid import loops.
 *
 * @param registry - The MessageRegistry to be injected
 */
export function setMessageRegistry(registry: MessageRegistry) {
  messageRegistry = registry;
}

const isLaoCreate = (m: ExtendedMessage) =>
  m.messageData.object === ObjectType.LAO && m.messageData.action === ActionType.CREATE;

function handleLaoCreateMessages(msg: ExtendedMessage): boolean {
  if (!isLaoCreate(msg)) {
    return false;
  }

  // processing the lao/create message:
  // - we either connect to the LAO (if there's no active connection) OR
  // - we simply add it to the list of known LAOs
  messageRegistry.handleMessage(msg);

  return true;
}

/**
 * Stores a received message
 * @param msg The message that should be stored
 */
export function storeMessage(msg: ExtendedMessage) {
  try {
    // process LAO/create message
    const isLao = handleLaoCreateMessages(msg);

    if (!isLao) {
      // send it to the store
      dispatch(addMessages(msg.toState()));
    }
  } catch (err) {
    console.warn('Messages could not be stored, error:', err, msg);
  }
}

export function handleExtendedRpcRequests(req: ExtendedJsonRpcRequest) {
  if (req.request.method === JsonRpcMethod.BROADCAST) {
    const broadcastParams = req.request.params as Broadcast;

    storeMessage(
      ExtendedMessage.fromMessage(
        broadcastParams.message,
        req.receivedFrom,
        broadcastParams.channel,
      ),
    );
  } else {
    console.warn('A request was received but it is currently unsupported:', req);
  }
}
