import { Channel } from 'core/objects';

import { storeMessage } from './ingestion';
import { catchup, subscribe } from './JsonRpcApi';
import { NetworkConnection } from './NetworkConnection';

export async function subscribeToChannel(channel: Channel, connections?: NetworkConnection[]) {
  if (!channel) {
    throw new Error('Could not subscribe to channel without a valid channel');
  }

  console.debug('Subscribing to channel: ', channel);

  try {
    // Subscribe to LAO main channel
    await subscribe(channel, connections);

    // Retrieve all previous LAO messages in the form of a generator
    const msgs = await catchup(channel, connections);

    for (const msg of msgs) {
      storeMessage(msg, channel);
    }

    return;
  } catch (err) {
    console.error('Something went wrong when subscribing to channel', err);
    throw err;
  }
}
