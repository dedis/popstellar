import { Channel } from 'core/objects';
import { OpenedLaoStore } from 'features/lao/store';
import { dispatch } from 'core/redux/';
import { addMessages } from './Reducer';
import { Broadcast } from '../jsonrpc/Broadcast';
import { ActionType, ObjectType } from '../jsonrpc/messages/MessageData';
import { MessageRegistry } from '../jsonrpc/messages';
import { Message } from '../jsonrpc/messages/Message';
import { JsonRpcMethod, JsonRpcRequest } from '../jsonrpc';
import { ExtendedMessage } from './ExtendedMessage';

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

export function storeMessage(msg: Message, ch: Channel) {
  try {
    // create extended message
    const extMsg = ExtendedMessage.fromMessage(msg, ch);

    // process LAO/create message
    const isLao = handleLaoCreateMessages(extMsg);

    if (!isLao) {
      // get the current LAO
      const laoId = OpenedLaoStore.get().id;

      // send it to the store
      dispatch(addMessages(laoId.valueOf(), extMsg.toState()));
    }
  } catch (err) {
    console.warn('Messages could not be stored, error:', err, msg);
  }
}

export function handleRpcRequests(req: JsonRpcRequest) {
  if (req.method === JsonRpcMethod.BROADCAST) {
    const broadcastParams = req.params as Broadcast;
    storeMessage(broadcastParams.message, broadcastParams.channel);
  } else {
    console.warn('A request was received but it is currently unsupported:', req);
  }
}
