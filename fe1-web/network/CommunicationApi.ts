import { Channel } from 'model/objects/Channel';
import { Message } from 'model/network/method/message';
import { catchup, subscribe } from 'network/JsonRpcApi';
import { storeMessages } from 'ingestion';

export function subscribeToChannel(channel: Channel): Promise<void> {
  if (!channel) {
    return Promise.reject(new Error('Could not suscribe to channel without a valid channel'));
  }

  console.info('Subscribing to channel: ', channel);
  // Subscribe to LAO main channel
  return subscribe(channel)
    // Retrieve all previous LAO messages in the form of a generator
    .then(() => catchup(channel))
    // Store them one at a time
    .then((msgs: Generator<Message, void, undefined>) => {
      for (const msg of msgs) {
        storeMessages(msg);
      }
    })
    // handle any error
    .catch((err) => {
      console.error('Something went wrong when subscribing to channel', err);
      throw err;
    });
}
