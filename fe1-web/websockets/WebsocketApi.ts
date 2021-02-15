import { Hash, Lao, PublicKey, Timestamp } from "Model/Objects";
import { JsonRpcMethod, JsonRpcRequest } from 'Model/Network';
import { Publish } from 'Model/Network/Method';
import { Message } from 'Model/Network/Method/Message';
import { MessageData } from 'Model/Network/Method/Message/data';
import { Channel } from "Model/Objects/Channel";

// ....
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

  sendRpc(JsonRpcRequest rpc) Promise<Answer> (...
    ongoing_rpc[rpc.id] = new Promise<Answer>();
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
  let message = Message.fromData(msgData);

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish({
        channel: channel,
        message: message
    }),
    id: -1, // FIXME
  });
/* // FIXME uncomment once websocket link is refactored
  WebsocketLink.sendRequestToServer(request,
    message.messageData.object,
    message.messageData.action);*/
}

