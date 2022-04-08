import { KeyPairRegistry } from 'core/keypair';
import { getNetworkManager } from 'core/network/NetworkManager';
import { Channel, KeyPair } from 'core/objects';

import { JsonRpcMethod, JsonRpcRequest, JsonRpcResponse, Publish, Subscribe } from './jsonrpc';
import { configureMessages, Message, MessageData, MessageRegistry } from './jsonrpc/messages';
import { NetworkConnection } from './NetworkConnection';

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
 * @param connections - An optional list of network connection if the message should only be sent on a subset of connections
 */
export async function publish(
  channel: Channel,
  msgData: MessageData,
  connections?: NetworkConnection[],
): Promise<void> {
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

  await getNetworkManager().sendPayload(request, connections);
}

/**
 * Subscribe to a channel
 *
 * @param channel - The channel to which we need to subscribe
 * @param connections - An optional list of network connection if the message should only be sent on a subset of connections
 */
export async function subscribe(
  channel: Channel,
  connections?: NetworkConnection[],
): Promise<void> {
  const request = new JsonRpcRequest({
    method: JsonRpcMethod.SUBSCRIBE,
    params: new Subscribe({
      channel: channel,
    }),
    id: AUTO_ASSIGN_ID,
  });

  await getNetworkManager().sendPayload(request, connections);
  // propagate the catch() with the full error message, as it needs to be handled on a higher level
}

function* messageGenerator(msgs: any[], channel: Channel) {
  for (const m of msgs) {
    yield Message.fromJson(m, channel);
  }
}

/**
 * Catch-up on the messages sent on the channel
 *
 * @param channel - The channel on which messages need to be retrieved
 * @param connections - An optional list of network connection if the message should only be sent on a subset of connections
 */
export async function catchup(
  channel: Channel,
  connections?: NetworkConnection[],
): Promise<Generator<Message, void, undefined>> {
  const request = new JsonRpcRequest({
    method: JsonRpcMethod.CATCHUP,
    params: new Subscribe({
      channel: channel,
    }),
    id: AUTO_ASSIGN_ID,
  });

  const responses: JsonRpcResponse[] = await getNetworkManager().sendPayload(request, connections);
  for (const response of responses) {
    // A JsonRpcResponse can have r.result being of type number or of Message[]
    // But in the case of a catchup we always expect Message[]
    if (typeof response.result === 'number') {
      console.log(
        'One of the received responses to a catchup message contained a number instead of a message array and will be ignored',
        response,
      );
    }
  }

  // only use responses containing a message array
  const validResponses = responses.filter((r) => typeof r.result !== 'number');
  if (validResponses.length === 0) {
    throw new Error('No responses containing messages were received after a catchup message!');
  }

  const msgs = [].concat(...validResponses.map((r) => r.result as any));
  return messageGenerator(msgs, channel);
}

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
