import { KeyPairStore } from 'store';
import { Channel, Hash } from 'core/objects';
import { publish } from 'core/network/jsonrpc/JsonRpcApi';

import { WitnessMessage } from './messages';

/**
 * Contains all functions to send witness related messages.
 */

/** Send a server messages to acknowledge witnessing the messages messages (JS object) */
export function requestWitnessMessage(channel: Channel, messageId: Hash): Promise<void> {
  const message = new WitnessMessage({
    message_id: messageId,
    signature: KeyPairStore.getPrivateKey().sign(messageId),
  });

  return publish(channel, message);
}
