import { JsonRpcMethod, JsonRpcRequest } from 'model/network';
import { Broadcast } from 'model/network/method';
import { Message, ExtendedMessage } from 'model/network/method/message';
import { dispatch, addMessages, OpenedLaoStore } from 'store';

export function storeMessages(...msgs: Message[]) {
  try {
    // get the current LAO
    const laoId = OpenedLaoStore.get().id;

    // create extended messages
    const extMsgs = msgs.map((m: Message) => ExtendedMessage.fromMessage(m).toState());

    // send it to the store
    dispatch(addMessages(laoId, extMsgs));
  } catch (err) {
    console.warn('Messages could not be stored, error:', err, msgs);
  }
}

export const storeMessage = (msg: Message) => storeMessages(msg);

export function handleRpcRequests(req: JsonRpcRequest) {
  if (req.method === JsonRpcMethod.BROADCAST) {
    storeMessage((req.params as Broadcast).message);
  } else {
    console.warn('A request was received but it is currently unsupported:', req);
  }
}
