import { JsonRpcMethod, JsonRpcRequest } from 'model/network';
import { Publish } from 'model/network/method';
import { Message } from 'model/network/method/message';
import { MessageData } from 'model/network/method/message/data';
import { Channel } from 'model/objects/Channel';
import { getNetworkManager } from 'network/NetworkManager';

export function publish(channel: Channel, msgData: MessageData) {
  const message = Message.fromData(msgData);

  const request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish({
      channel,
      message,
    }),
    id: -1, // FIXME
  });

  getNetworkManager().sendPayload(request); // FIXME ignoring promise for now
}
