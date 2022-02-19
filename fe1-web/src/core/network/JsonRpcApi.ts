import { Channel, ProtocolError } from 'core/objects';
import { getNetworkManager } from 'core/network/NetworkManager';
import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';

import { JsonRpcMethod, JsonRpcRequest, JsonRpcResponse, Publish, Subscribe } from './jsonrpc';
import { Message, MessageData, MessageRegistry } from './jsonrpc/messages';

export const AUTO_ASSIGN_ID = -1;
let messageRegistry: MessageRegistry;
let keyPairRegistry: KeyPairRegistry;

/**
 * Dependency injection of a MessageRegistry and a KeyPairRegistry.
 *
 * @param messageReg - The MessageRegistry to be injected
 * @param keyPairReg - The KeyPairRegistry to be injected
 */
export function setSignatureKeyPair(messageReg: MessageRegistry, keyPairReg: KeyPairRegistry) {
  messageRegistry = messageReg;
  keyPairRegistry = keyPairReg;
}

export async function publish(channel: Channel, msgData: MessageData): Promise<void> {
  const signature = messageRegistry.getSignatureType(msgData);
  const keyPair = await keyPairRegistry.getSignatureKeyPair(signature);

  const message = await Message.fromData(msgData, keyPair);
  const request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish({
      channel: channel,
      message: message,
    }),
    id: AUTO_ASSIGN_ID,
  });

  await getNetworkManager().sendPayload(request);
}

export function subscribe(channel: Channel): Promise<void> {
  const request = new JsonRpcRequest({
    method: JsonRpcMethod.SUBSCRIBE,
    params: new Subscribe({
      channel: channel,
    }),
    id: AUTO_ASSIGN_ID,
  });

  return getNetworkManager()
    .sendPayload(request)
    .then(() => {
      /* discard JsonRpcResponse, as subscribe only returns an ack */
    });
  // propagate the catch() with the full error message, as it needs to be handled on a higher level
}

function* messageGenerator(msgs: any[]) {
  for (const m of msgs) {
    yield Message.fromJson(m);
  }
}

export async function catchup(channel: Channel): Promise<Generator<Message, void, undefined>> {
  const request = new JsonRpcRequest({
    method: JsonRpcMethod.CATCHUP,
    params: new Subscribe({
      channel: channel,
    }),
    id: AUTO_ASSIGN_ID,
  });

  // do not catch, as it needs to be handled on a higher level
  const response: JsonRpcResponse = await getNetworkManager().sendPayload(request);
  if (typeof response.result === 'number') {
    throw new Error('FIXME number in result. Should it be here?');
  }

  const msgs = response.result as any[];
  return messageGenerator(msgs);
}
