import { JsonRpcMethod, JsonRpcRequest } from 'model/network';
import { Publish } from 'model/network/method';
import { Message } from 'model/network/method/message';
import { MessageData } from 'model/network/method/message/data';
import { Channel } from 'model/objects/Channel';

/*
class Manager (
  WebSockets[]

  connect(...)
  disconnect(...)

  sendRpc(JsonRpcRequest rpc) --> ws[0].sendRpc(rpc).catch(err) (
    return new Error(...)
)
)

class WebSocket (
  ongoing_id: number[];
  ongoing_rpc: map ( id ) -> Promise
  next_id: number;

  sendRpc(JsonRpcRequest rpc) Promise<answer> (...
    ongoing_rpc[rpc.id] = new Promise<answer>();
  )

  onMessage(rpc) (
    ongoing_rpc[id].resolve(...)
  )
)

export function catchup(channel: Channel) {

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.CATCHUP,
    params: new Publish({
      channel: channel,
    }),
    id: GENERATE_ID, // FIXME
  });
  /* // FIXME uncomment once websocket link is refactored
    WebsocketLink.sendRequestToServer(request,
      message.messageData.object,
      message.messageData.action);*
}
*/
export function publish(channel: Channel, msgData: MessageData) {
  const message = Message.fromData(msgData);

  // FIXME
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish({
      channel,
      message,
    }),
    id: -1, // FIXME
  });

/* // FIXME uncomment once websocket link is refactored
  WebsocketLink.sendRequestToServer(request,
    message.messageData.object,
    message.messageData.action); */
}
