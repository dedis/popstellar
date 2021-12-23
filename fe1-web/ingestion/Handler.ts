import { JsonRpcMethod, JsonRpcRequest } from 'model/network';
import { Broadcast } from 'model/network/method';
import { ExtendedMessage, Message } from 'model/network/method/message';
import { ActionType, ObjectType } from 'model/network/method/message/data';
import { Channel } from 'model/objects';
import {
  addMessages, dispatch, OpenedLaoStore,
} from 'store';
import { handleLaoMessage } from './handlers';

const isLaoCreate = (m: ExtendedMessage) => m.messageData.object === ObjectType.LAO
  && m.messageData.action === ActionType.CREATE;

function handleLaoCreateMessages(msg: ExtendedMessage) : boolean {
  if (!isLaoCreate(msg)) {
    return false;
  }

  // processing the lao/create message:
  // - we either connect to the LAO (if there's no active connection) OR
  // - we simply add it to the list of known LAOs
  handleLaoMessage(msg);

  return true;
}

export function storeMessage(msg: Message, ch: Channel) {
  try {
    // create extended messages
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
    const broadcastParams = (req.params as Broadcast);
    storeMessage(broadcastParams.message, broadcastParams.channel);
  } else {
    console.warn('A request was received but it is currently unsupported:', req);
  }
}
