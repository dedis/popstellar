import { Channel } from 'model/objects/Channel';
import { Message } from 'model/network/method/message';
import { catchup, subscribe } from 'network/JsonRpcApi';
import { storeMessages } from 'ingestion';

export function establishLaoConnection(channel: Channel): Promise<void> {
  if (!channel) {
    return Promise.reject(new Error('Could not establish LAO connection without a valid channel'));
  }

  console.info('Subscribing to channel: ', channel);
  // Subscribe to LAO main channel
  return subscribe(channel)
    // Retrieve all previous LAO messages
    .then(() => catchup(channel).then((messages: Message[]) => storeMessages(...messages)))
    // handle any error
    .catch((err) => {
      console.error('Something went wrong when establishing the LAO connection', err);
      throw err;
    });
}
