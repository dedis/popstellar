import { JsonRpcResponse } from 'core/network/jsonrpc';
import { NetworkConnection } from 'core/network/NetworkConnection';
import { NetworkError } from 'core/network/NetworkError';

import { SendingStrategy } from './ClientMultipleServerStrategy';

export const sendToFirstAcceptingServerStrategy: SendingStrategy = async (payload, connections) => {
  if (connections.length === 0) {
    throw new NetworkError('Cannot send payload: no websocket connection available');
  }

  try {
    // .slice(1) ignores the first connection as this is the input to our reduce()
    // then we just add a .catch() for consecutive connections s.t.
    // we try the next connection in the array on failure
    const response = await connections
      .slice(1) // ignore the first connection in the array
      .reduce<Promise<JsonRpcResponse>>( // assert the correct type
        async (promise: Promise<JsonRpcResponse>, connection: NetworkConnection) => {
          // try to resolve the previous connection and see whether we get a
          // response
          try {
            // if we do, just return the response
            return await promise;
          } catch (error) {
            // in case the previous connection failed, try the next one
            console.warn(`Could not send payload to ${connection.address} due to failure:`, error);
            console.warn('Trying a different connection');
            return connection.sendPayload(payload);
          }
        },
        // initially we just send the payload to the first server
        connections[0].sendPayload(payload),
      );

    return [response];
  } catch (error) {
    console.error('Could not send payload on any connection due to failure:', error);
    throw error;
  }
};
