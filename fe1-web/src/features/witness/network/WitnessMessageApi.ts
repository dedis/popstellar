import { KeyPairStore } from 'core/keypair';
import { Channel, Hash } from 'core/objects';
import { publish } from 'core/network/JsonRpcApi';

import { WitnessMessage } from './messages';

/**
 * Contains all functions to send witness related messages.
 */

/** Send a server message to acknowledge witnessing the message (JS object) */
export function requestWitnessMessage(channel: Channel, messageId: Hash): Promise<void> {
  const message = new WitnessMessage({
    message_id: messageId,
    signature: KeyPairStore.getPrivateKey().sign(messageId),
  });

  return publish(channel, message);
}
