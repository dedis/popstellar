import { KeyPairStore } from 'store';
import { Channel, Hash } from 'model/objects';
import { publish } from 'network/JsonRpcApi';

import { WitnessMessage } from './messages';

/**
 * Contains all functions to send witness related messages.
 */

/** Send a server message to acknowledge witnessing the message message (JS object) */
export function requestWitnessMessage(channel: Channel, messageId: Hash): Promise<void> {
  const message = new WitnessMessage({
    message_id: messageId,
    signature: KeyPairStore.getPrivateKey().sign(messageId),
  });

  return publish(channel, message);
}
