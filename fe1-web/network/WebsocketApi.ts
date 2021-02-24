import { JsonRpcMethod, JsonRpcRequest } from 'model/network';
import { Publish } from 'model/network/method';
import { Message } from 'model/network/method/message';
import { MessageData } from 'model/network/method/message/data';
import { Channel } from 'model/objects/Channel';
import { getNetworkManager } from 'network/NetworkManager';

export function publish(channel: Channel, msgData: MessageData): Promise<void> {
  const message = Message.fromData(msgData);

  const request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish({
      channel,
      message,
    }),
    id: -1, // FIXME
  });

  return getNetworkManager().sendPayload(request)
    .then(() => { /* discard JsonRpcResponse, as publish only returns an ack */ });
  // propagate the catch() with the full error message, as it needs to be handled on a higher level
}
