import { Channel } from 'model/objects/Channel';
import { catchup, subscribe } from 'network/JsonRpcApi';
import { storeMessage } from 'ingestion';

export async function subscribeToChannel(channel: Channel) {
  if (!channel) {
    throw new Error('Could not subscribe to channel without a valid channel');
  }

  console.debug('Subscribing to channel: ', channel);

  try {
    // Subscribe to LAO main channel
    await subscribe(channel);

    // Retrieve all previous LAO messages in the form of a generator
    const msgs = await catchup(channel);

    for (const msg of msgs) {
      storeMessage(msg, channel);
    }

    return;
  } catch (err) {
    console.error('Something went wrong when subscribing to channel', err);
    throw err;
  }
}
