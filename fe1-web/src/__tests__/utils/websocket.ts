import {
  w3cwebsocket,
  // @ts-ignore
  addNetworkMessageHandler as addHandler,
  // @ts-ignore
  removeNetworkMessageHandler as removeHandler,
  // @ts-ignore
  clearNetworkMessageHandlers as clearHandlers,
  // @ts-ignore
  allowNewConnections as allowNewC,
  // @ts-ignore
  disallowNewConnections as disallowNewC,
} from 'websocket';

export type MockWebsocket = w3cwebsocket & {
  mockReceive: (message: NetworkMessage) => void;
  mockConnectionClose: (wasClean: boolean, code?: number, reason?: string) => void;
};

export type NetworkMessage = string;
export type NetworkMessageHandler = (ws: MockWebsocket, message: NetworkMessage) => void;

export const addNetworkMessageHandler = addHandler as (handler: NetworkMessageHandler) => void;
export const removeNetworkMessageHandler = removeHandler as (
  handler: NetworkMessageHandler,
) => void;
export const clearNetworkMessageHandlers = clearHandlers as () => void;

export const allowNewConnections = allowNewC as () => void;
export const disallowNewConnections = disallowNewC as () => void;
