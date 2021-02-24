import { JsonRpcMethod, JsonRpcRequest } from 'model/network';
import { Broadcast } from 'model/network/method';
import { Message } from 'model/network/method/message';

/** Processes the message and sends it to storage
 *
 * The purpose of this method is to decouple the communication protocol from the storage layer.
 *
 * @param msg a Broadcast message
 */
function handleBroadcastMessage(msg: Message) {
  // Algorithm sketch:
  // - process the message
  // - transform it into a model/object as needed
  // - send it to storage

  // e.g. Lao/Create -> Lao -> dispatch ADD_LAO, SET_OPENED_LAO
  // e.g. RollCall/Open -> validate -> RollCall (updated) -> dispatch UPDATE_EVENT

  console.warn('A message broadcast was received but'
    + ' its processing logic is not yet implemented:', msg);
}

export function handleRpcRequests(req: JsonRpcRequest) {
  if (req.method === JsonRpcMethod.BROADCAST) {
    handleBroadcastMessage((req.params as Broadcast).message);
  } else {
    console.warn('A request was received but it is currently unsupported:', req);
  }
}
