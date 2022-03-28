import { NetworkError } from 'core/network/NetworkError';

import { SendingStrategy } from './ClientMultipleServerStrategy';

/**
 * This is the legacy implementation that only sends the request to the first server in the list
 */
export const sendToFirstServerStrategy: SendingStrategy = async (payload, connections) => {
  if (connections.length === 0) {
    throw new NetworkError('Cannot send payload: no websocket connection available');
  }

  try {
    return [await connections[0].sendPayload(payload)];
  } catch (error) {
    // Some day, we will want to retry from a different network connection,
    // before throwing an error to the caller
    console.error('Could not send payload due to failure:', error);
    throw error;
  }
};
