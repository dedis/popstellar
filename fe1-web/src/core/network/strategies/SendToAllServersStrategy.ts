import { NetworkError } from 'core/network/NetworkError';

import { SendingStrategy } from './ClientMultipleServerStrategy';

export const sendToAllServersStrategy: SendingStrategy = async (payload, connections) => {
  if (connections.length === 0) {
    throw new NetworkError('Cannot send payload: no websocket connection available');
  }

  try {
    return await Promise.all(connections.map((c) => c.sendPayload(payload)));
  } catch (error) {
    console.error('Could not send payload due to failure:', error);
    throw error;
  }
};
