import { Hash } from 'model/objects';
import { WitnessMessage } from 'model/network/method/message/data';
import { Channel } from 'model/objects/Channel';
import { KeyPairStore } from 'store';
import { publish } from './JsonRpcApi';

/** Send a server message to acknowledge witnessing the message message (JS object) */
export function requestWitnessMessage(channel: Channel, messageId: Hash): Promise<void> {
  const message = new WitnessMessage({
    message_id: messageId,
    signature: KeyPairStore.getPrivateKey().sign(messageId),
  });

  return publish(channel, message);
}
