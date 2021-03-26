import { JsonRpcMethod, JsonRpcRequest } from 'model/network';
import { Broadcast } from 'model/network/method';
import { ExtendedMessage, Message } from 'model/network/method/message';
import { ActionType, CreateLao, ObjectType } from 'model/network/method/message/data';
import {
  addMessages, connectToLao, dispatch, OpenedLaoStore,
} from 'store';
import { Lao } from 'model/objects';

const isLaoCreate = (m: ExtendedMessage) => m.messageData.object === ObjectType.LAO
  && m.messageData.action === ActionType.CREATE;

function handleLaoCreateMessages(msgs: ExtendedMessage[]) : ExtendedMessage[] {
  const returnedMsgs: ExtendedMessage[] = [];

  msgs.forEach((m) => {
    if (!isLaoCreate(m)) {
      returnedMsgs.push(m);
      return;
    }

    const msg = ((m.messageData) as CreateLao);

    const lao = new Lao({
      id: msg.id,
      name: msg.name,
      creation: msg.creation,
      last_modified: msg.creation,
      organizer: msg.organizer,
      witnesses: msg.witnesses,
    });

    // we either connect to the LAO (if there's no active connection)
    // or we simply add it to the list of known LAOs
    dispatch(connectToLao(lao.toState()));
  });

  return returnedMsgs;
}

export function storeMessages(...msgs: Message[]) {
  try {
    // create extended messages
    const extMsgs = msgs.map((m: Message) => ExtendedMessage.fromMessage(m));

    // process LAO/create messages and return array without them
    const otherMsgs = handleLaoCreateMessages(extMsgs);

    // get the current LAO
    const laoId = OpenedLaoStore.get().id;

    // send it to the store
    const msgStates = otherMsgs.map((m) => m.toState());
    dispatch(addMessages(laoId, msgStates));
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
