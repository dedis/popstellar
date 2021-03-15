import { requestCreateLao } from 'network/MessageApi';
import { Hash } from 'model/objects';
import { channelFromId } from 'model/objects/Channel';
import { catchup, subscribe } from 'network/JsonRpcApi';
import { storeMessages } from 'ingestion';
import { Message } from 'model/network/method/message';
import { getNetworkManager } from 'network/NetworkManager';

export function establishLaoConnection(name: string): Promise<void> {
  if (!name) {
    return Promise.reject(new Error('Could not create LAO without a name'));
  }

  getNetworkManager().connect('127.0.0.1', 8080);

  // Create a LAO
  return requestCreateLao(name).then((id: Hash) => {
    const channel = channelFromId(id);
    console.info('Subscribing to channel: ', channel);

    // Subscribe to LAO main channel
    return subscribe(channel)
      // Retrieve all previous LAO messages
      .then(() => catchup(channel)
        .then((messages: Message[]) => storeMessages(...messages)));
  }).catch((err) => {
    console.error('Something went horribly wrong when establishing the LAO connection', err);
  });
}
