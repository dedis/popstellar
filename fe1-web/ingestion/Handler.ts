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
}

export function handleRpcRequests(req: JsonRpcRequest) {
  switch (req.method) {
    case JsonRpcMethod.BROADCAST:
      handleBroadcastMessage((req.params as Broadcast).message);
      break;

    default:
      // To be continued... log unsupported requests
      break;
  }
}
