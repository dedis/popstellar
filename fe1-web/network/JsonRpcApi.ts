import { JsonRpcMethod, JsonRpcRequest, JsonRpcResponse } from 'model/network';
import { Publish, Subscribe } from 'model/network/method';
import { Message } from 'model/network/method/message';
import { MessageData } from 'model/network/method/message/data';
import { Channel } from 'model/objects/Channel';
import { getNetworkManager } from './NetworkManager';

export const AUTO_ASSIGN_ID = -1;

export function publish(channel: Channel, msgData: MessageData): Promise<void> {
  const message = Message.fromData(msgData);

  const request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish({
      channel: channel,
      message: message,
    }),
    id: AUTO_ASSIGN_ID,
  });

  return getNetworkManager().sendPayload(request)
    .then(() => { /* discard JsonRpcResponse, as publish only returns an ack */ });
  // propagate the catch() with the full error message, as it needs to be handled on a higher level
}

export function subscribe(channel: Channel): Promise<void> {
  const request = new JsonRpcRequest({
    method: JsonRpcMethod.SUBSCRIBE,
    params: new Subscribe({
      channel: channel,
    }),
    id: AUTO_ASSIGN_ID,
  });

  return getNetworkManager().sendPayload(request)
    .then(() => { /* discard JsonRpcResponse, as subscribe only returns an ack */ });
  // propagate the catch() with the full error message, as it needs to be handled on a higher level
}

export function catchup(channel: Channel): Promise<Message[]> {
  const request = new JsonRpcRequest({
    method: JsonRpcMethod.CATCHUP,
    params: new Subscribe({
      channel: channel,
    }),
    id: AUTO_ASSIGN_ID,
  });

  return getNetworkManager().sendPayload(request).then(
    (r: JsonRpcResponse) => {
      if (typeof r.result === 'number') {
        throw new Error('FIXME number in result. Should it be here?');
      }

      // FIXME: massive bug here. cannot `fromJson`
      return (r.result as any[]).map((m) => Message.fromJson(m));
    },
  );
  // propagate the catch() with the full error message, as it needs to be handled on a higher level
}
