import { describe, expect } from '@jest/globals';

import { mockAddress, mockChannel } from '__tests__/utils';
import {
  addNetworkMessageHandler,
  allowNewConnections,
  clearNetworkMessageHandlers,
  disallowNewConnections,
  MockWebsocket,
} from '__tests__/utils/websocket';

import {
  ExtendedJsonRpcRequest,
  ExtendedJsonRpcResponse,
  JsonRpcMethod,
  JsonRpcRequest,
  JsonRpcResponse,
  Subscribe,
} from '../jsonrpc';
import { AUTO_ASSIGN_ID } from '../JsonRpcApi';
import { NetworkConnection } from '../NetworkConnection';
import { NetworkError } from '../NetworkError';
import { RpcOperationError } from '../RpcOperationError';

// mock network connections
jest.mock('websocket');

// websocket ready states
const CONNECTING = 0;
const OPEN = 1;
const CLOSING = 2;
const CLOSED = 3;

const mockRequest = new JsonRpcRequest({
  method: JsonRpcMethod.SUBSCRIBE,
  params: new Subscribe({
    channel: mockChannel,
  }),
  id: AUTO_ASSIGN_ID,
});

beforeEach(() => {
  allowNewConnections();
  clearNetworkMessageHandlers();
});

// define a function that allows us retrieving the websocket from a
// connection object

const getWebsocket = (connection: NetworkConnection): MockWebsocket =>
  // @ts-ignore
  // eslint-disable-next-line @typescript-eslint/dot-notation
  connection['ws'];

describe('NetworkConnection', () => {
  describe('constructor', () => {
    it('is possible to construct new instances', async () => {
      const connection = await new Promise<NetworkConnection>((resolve) => {
        const c: NetworkConnection = new NetworkConnection(mockAddress, undefined, () =>
          resolve(c),
        );

        expect(getWebsocket(c).readyState).toEqual(CONNECTING);
        expect(c).toBeInstanceOf(NetworkConnection);
        expect(c.address).toEqual(mockAddress);
      });

      const ws = getWebsocket(connection);

      expect(ws.readyState).toEqual(OPEN);
    });

    it('calls callback if the the connection times out', async () => {
      const connection = await new Promise<NetworkConnection>((resolve, reject) => {
        const c: NetworkConnection = new NetworkConnection(
          mockAddress,
          undefined,
          reject,
          () => resolve(c),
          undefined,
          0,
          0,
        );
      });

      const ws = getWebsocket(connection);

      expect([CLOSING, CLOSED]).toContainEqual(ws.readyState);
    });

    it('calls callback if the the connection times out even if no callback is provided', async () => {
      const connection = await new Promise<NetworkConnection>((resolve, reject) => {
        const c = new NetworkConnection(mockAddress, undefined, reject, undefined, undefined, 0, 0);
        // this is called *after* the 0 timeout
        setTimeout(() => resolve(c), 1);
      });

      // so here we should have a closed websocket connection
      const ws = getWebsocket(connection);
      expect([CLOSING, CLOSED]).toContainEqual(ws.readyState);
    });
  });

  describe('create', () => {
    it('resolves to a connection if it is possible to connect', async () => {
      allowNewConnections();
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkConnection = connection[0];
      const networkError = connection[1];

      expect(networkConnection).toBeInstanceOf(NetworkConnection);
      expect(networkConnection.address).toEqual(mockAddress);
      expect(getWebsocket(networkConnection).readyState).toEqual(OPEN);
      expect(networkError).toBeNull();
    });

    /**
     * This test might log some errors depending on the timeout for the initial connection
     * and the time configured for the mocked websockets
     */
    it('return networkError if it is not possible to create the connection within the timeout', async () => {
      disallowNewConnections();

      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkError = connection[1];

      expect(networkError).toBeInstanceOf(NetworkError);
    });

    jest.setTimeout(20000);
    it('calls onConnectionDeathCallback if the connection breaks for good', async () => {
      const onDeath = jest.fn();

      const connection = await NetworkConnection.create(mockAddress, jest.fn(), onDeath);
      const networkConnection = connection[0];

      // after a successful connection, prevent new ones
      disallowNewConnections();

      // reduce timeout to zero
      // @ts-ignore
      // eslint-disable-next-line @typescript-eslint/dot-notation
      connection['websocketConnectionTimeout'] = 0;

      // and trigger a close
      getWebsocket(networkConnection).mockConnectionClose(false);

      // this should now trigger a loop of re-connects
      // and at some point call "onDeath"

      await new Promise((resolve) => setTimeout(resolve, 6000));

      expect(onDeath).toHaveBeenCalledTimes(1);
    });
  });

  describe('reconnectIfNecessary', () => {
    it('does nothing if the socket is still open', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkConnection = connection[0];

      const ws = getWebsocket(networkConnection);
      await expect(networkConnection.reconnectIfNecessary()).resolves.not.toThrow();

      // should still be the same websocket object
      expect(getWebsocket(networkConnection)).toBe(ws);
    });

    it('re-connects if the socket has been closed', async () => {
      // This simulates the situation where the readyState of the websocket has been updated
      // because it is no longer open but onClose was not called. Yes, this should in theory never
      // happen.
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkConnection = connection[0];

      const ws = getWebsocket(networkConnection);
      ws.readyState = 3;
      await expect(networkConnection.reconnectIfNecessary()).resolves.not.toThrow();

      const ws2 = getWebsocket(networkConnection);

      // the websocket connection should have been replaced
      expect(ws2).not.toBe(ws);
      expect(ws2.readyState).toEqual(OPEN);
    });

    it('rejects if the connection times out', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkConnection = connection[0];

      // reduce timeout to zero
      // @ts-ignore
      // eslint-disable-next-line @typescript-eslint/dot-notation
      networkConnection['websocketConnectionTimeout'] = 0;

      const ws = getWebsocket(networkConnection);
      ws.readyState = 3;
      await expect(networkConnection.reconnectIfNecessary()).rejects.toBeInstanceOf(NetworkError);
    });
  });

  describe('setRpcHandler', () => {
    it('allows setting a new rpc handler', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkConnection = connection[0];

      const newHandler = jest.fn();
      networkConnection.setRpcHandler(newHandler);

      const ws = getWebsocket(networkConnection);

      ws.mockReceive(JSON.stringify(mockRequest));

      expect(newHandler).toHaveBeenCalledWith(
        new ExtendedJsonRpcRequest({ receivedFrom: mockAddress }, mockRequest),
      );
    });
  });

  describe('sendPayload', () => {
    it('allows sending a payload', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkConnection = connection[0];

      addNetworkMessageHandler((ws, msg) => {
        const message = JSON.parse(msg);
        const id = message.id as number;

        const mockResponse = new JsonRpcResponse({
          id,
          result: 0,
        });

        ws.mockReceive(JSON.stringify(mockResponse));
      });

      const promise = networkConnection.sendPayload(mockRequest);

      await expect(promise).resolves.toBeInstanceOf(ExtendedJsonRpcResponse);
      await expect(promise).resolves.toHaveProperty('response.result', 0);
    });

    jest.setTimeout(20000);
    it('return network error if the sending times out', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkConnection = connection[0];

      // reduce timeout to zero
      // @ts-ignore
      // eslint-disable-next-line @typescript-eslint/dot-notation
      connection['websocketMessageTimeout'] = 0;

      const promise = networkConnection.sendPayload(mockRequest);

      await expect(promise).rejects.toBeInstanceOf(NetworkError);
    });

    it('rejects if an error is received', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkConnection = connection[0];

      addNetworkMessageHandler((ws, msg) => {
        const message = JSON.parse(msg);
        const id = message.id as number;

        const mockResponse = new JsonRpcResponse({
          id,
          error: {
            code: -1,
            description: 'some description',
          },
        });

        ws.mockReceive(JSON.stringify(mockResponse));
      });

      await expect(networkConnection.sendPayload(mockRequest)).rejects.toBeInstanceOf(
        RpcOperationError,
      );
    });

    it('waits until the websocket connection is ready / open', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkConnection = connection[0];

      const networkMessageHandler = jest.fn<void, [MockWebsocket, string]>((ws, msg) => {
        const message = JSON.parse(msg);
        const id = message.id as number;

        const mockResponse = new JsonRpcResponse({
          id,
          result: 0,
        });

        ws.mockReceive(JSON.stringify(mockResponse));
      });

      addNetworkMessageHandler(networkMessageHandler);

      // make connection think the socket is still connecting
      getWebsocket(networkConnection).readyState = 0;

      const promise = networkConnection.sendPayload(mockRequest);

      // check if message has already been sent
      expect(networkMessageHandler).toHaveBeenCalledTimes(0);

      // wait 100 ms
      await new Promise((resolve) => setTimeout(resolve, 100));

      // now mock connection establishment
      getWebsocket(networkConnection).readyState = 1;

      // now the promise should resolve
      await expect(promise).resolves.toBeInstanceOf(ExtendedJsonRpcResponse);
      await expect(promise).resolves.toHaveProperty('response.result', 0);
    });

    it('retries sending after a new connection is established', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());
      const networkConnection = connection[0];

      // send some payload that takes a looooong time
      const promise = networkConnection.sendPayload(mockRequest);

      // add network handler aferwards, we don't want to see the above
      // message, only the one re-sent

      addNetworkMessageHandler((ws, msg) => {
        const message = JSON.parse(msg);
        const id = message.id as number;

        const mockResponse = new JsonRpcResponse({
          id,
          result: 0,
        });

        ws.mockReceive(JSON.stringify(mockResponse));
      });

      // force re-connect / simulate connection breaking
      getWebsocket(networkConnection).readyState = 3;
      await expect(networkConnection.reconnectIfNecessary()).resolves.not.toThrow();

      // expect the promise to resolve with the re-sent message
      await expect(promise).resolves.toBeInstanceOf(ExtendedJsonRpcResponse);
      await expect(promise).resolves.toHaveProperty('response.result', 0);
    });
  });
});
