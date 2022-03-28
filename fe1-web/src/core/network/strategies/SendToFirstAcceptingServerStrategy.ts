import { ExtendedJsonRpcResponse } from 'core/network/jsonrpc';
import { NetworkError } from 'core/network/NetworkError';

import { SendingStrategy } from './ClientMultipleServerStrategy';

export const sendToFirstAcceptingServerStrategy: SendingStrategy = async (payload, connections) => {
  if (connections.length === 0) {
    throw new NetworkError('Cannot send payload: no websocket connection available');
  }

  let response: ExtendedJsonRpcResponse | null = null;
  for (const connection of connections) {
    // try to send the payload using the current connection
    try {
      // eslint-disable-next-line no-await-in-loop
      response = await connection.sendPayload(payload);
      // in case it works, break out from the loop
      break;
    } catch (error) {
      // in case the connection failed, try the next one
      console.info(`Could not send payload to ${connection.address} due to failure:`, error);
      console.info('Trying a different connection');
    }
  }

  if (!response) {
    throw new NetworkError('Could not send payload on any connection due to failure');
  }

  return [response];
};
