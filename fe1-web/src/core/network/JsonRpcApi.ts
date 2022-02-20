import { Channel, KeyPair } from 'core/objects';
import { getNetworkManager } from 'core/network/NetworkManager';
import { KeyPairRegistry } from 'core/keypair';

import { JsonRpcMethod, JsonRpcRequest, JsonRpcResponse, Publish, Subscribe } from './jsonrpc';
import { configureMessages, Message, MessageData, MessageRegistry } from './jsonrpc/messages';

export const AUTO_ASSIGN_ID = -1;

/**
 * A local reference to the global MessageRegistry object
 */
let messageRegistry: MessageRegistry;

/**
 * A local reference to the global KeyPairRegistry object
 */
let keyPairRegistry: KeyPairRegistry;

/**
 * Configure the JSON-RPC interface with its dependencies
 *
 * @param messageReg - The MessageRegistry to be injected
 * @param keyPairReg - The KeyPairRegistry to be injected
 */
export function configureJsonRpcApi(messageReg: MessageRegistry, keyPairReg: KeyPairRegistry) {
  messageRegistry = messageReg;
  keyPairRegistry = keyPairReg;

  configureMessages(messageReg);
}

/**
 * Get the keypair with which to sign the MessageData
 *
 * @param msgData - The MessageData to be signed
 */
export function getSigningKeyPair(msgData: MessageData): Promise<KeyPair> {
  const signature = messageRegistry.getSignatureType(msgData);
  return keyPairRegistry.getSignatureKeyPair(signature);
}

/**
 * Publish a message on the channel
 *
 * @param channel - The channel on which to publish the message
 * @param msgData - The message data to be sent on the channel
 */
export async function publish(channel: Channel, msgData: MessageData): Promise<void> {
  const keyPair = await getSigningKeyPair(msgData);
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

/**
 * Subscribe to a channel
 *
 * @param channel - The channel to which we need to subscribe
 */
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

/**
 * Catch-up on the messages sent on the channel
 *
 * @param channel - The channel on which messages need to be retrieved
 */
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
