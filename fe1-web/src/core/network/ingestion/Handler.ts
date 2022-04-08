import { Server } from 'core/objects';
import { dispatch } from 'core/redux';
import { addServer } from 'core/redux/ServerReducer';

import { Broadcast, JsonRpcMethod, ExtendedJsonRpcRequest } from '../jsonrpc';
import { Greeting } from '../jsonrpc/Greeting';
import { ActionType, MessageRegistry, ObjectType } from '../jsonrpc/messages';
import { getNetworkManager } from '../NetworkManager';
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
        broadcastParams.channel,
        req.receivedFrom,
      ),
    );
  } else if (req.request.method === JsonRpcMethod.GREETING) {
    const greetingParams = req.request.params as Greeting;

    dispatch(
      addServer(
        new Server({ address: greetingParams.address, publicKey: greetingParams.sender }).toState(),
      ),
    );

    // connect to all received peer addresses
    // IMPORTANT: The network manager deduplicates connections to the same address (string)
    // and the received peer addresses are supposed to be the canonical ones.
    // Hence it just has to be made sure that the first connection also is to the canonical
    // address, otherwiese a client will connect to the same server twice (e.g. using its IP and then
    // then using the canonical domain address)
    const networkManager = getNetworkManager();
    for (const peerAddress of greetingParams.peers) {
      networkManager.connect(peerAddress);
    }
  } else {
    console.warn('A request was received but it is currently unsupported:', req);
  }
}
