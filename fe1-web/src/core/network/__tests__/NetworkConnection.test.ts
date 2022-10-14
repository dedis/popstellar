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

// mock network connections
jest.mock('websocket');

// websocket ready states
const CONNECTING = 0;
const OPEN = 1;

const mockRequest = new JsonRpcRequest({
  method: JsonRpcMethod.SUBSCRIBE,
  params: new Subscribe({
    channel: mockChannel,
  }),
  id: AUTO_ASSIGN_ID,
});

beforeEach(() => {
  // for some reason having 'allowNewConnections();' in here 'breaks' the tests..?
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
    beforeAll(() => {
      allowNewConnections();
    });

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
  });

  describe('create', () => {
    it('resolves to a connection if it is possible to connect', async () => {
      allowNewConnections();
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());

      expect(connection).toBeInstanceOf(NetworkConnection);
      expect(connection.address).toEqual(mockAddress);
      expect(getWebsocket(connection).readyState).toEqual(OPEN);
    });

    /**
     * This test might log some errors depending on the timeout for the initial connection
     * and the time configured for the mocked websockets
     */
    it('rejects of it is not possible to create the connection within the timeout', async () => {
      disallowNewConnections();

      await expect(
        NetworkConnection.create(mockAddress, jest.fn(), jest.fn()),
      ).rejects.toBeInstanceOf(Error);
    });
  });

  describe('reconnectIfNecessary', () => {
    beforeAll(() => {
      allowNewConnections();
    });

    it('does nothing if the socket is still open', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());

      const ws = getWebsocket(connection);
      await expect(connection.reconnectIfNecessary()).resolves.not.toThrow();

      // should still be the same websocket object
      expect(getWebsocket(connection)).toBe(ws);
    });
    it('re-connects if the socket has been closed', async () => {
      // This simulates the situation where the readyState of the websocket has been updated
      // because it is no longer open but onClose was not called. Yes, this should in theory never
      // happen.
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());

      const ws = getWebsocket(connection);
      ws.readyState = 3;
      await expect(connection.reconnectIfNecessary()).resolves.not.toThrow();

      const ws2 = getWebsocket(connection);

      // the websocket connection should have been replaced
      expect(ws2).not.toBe(ws);
      expect(ws2.readyState).toEqual(OPEN);
    });
  });

  describe('setRpcHandler', () => {
    beforeAll(() => {
      allowNewConnections();
    });

    it('allows setting a new rpc handler', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());

      const newHandler = jest.fn();
      connection.setRpcHandler(newHandler);

      const ws = getWebsocket(connection);

      ws.mockReceive(JSON.stringify(mockRequest));

      expect(newHandler).toHaveBeenCalledWith(
        new ExtendedJsonRpcRequest({ receivedFrom: mockAddress }, mockRequest),
      );
    });
  });

  describe('sendPayload', () => {
    beforeAll(() => {
      allowNewConnections();
    });

    it('allows sending a payload', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());

      addNetworkMessageHandler((ws, msg) => {
        const message = JSON.parse(msg);
        const id = message.id as number;

        const mockResponse = new JsonRpcResponse({
          id,
          result: 0,
        });

        ws.mockReceive(JSON.stringify(mockResponse));
      });

      const promise = connection.sendPayload(mockRequest);

      await expect(promise).resolves.toBeInstanceOf(ExtendedJsonRpcResponse);
      await expect(promise).resolves.toHaveProperty('response.result', 0);
    });

    it('waits until the websocket connection is ready / open', async () => {
      const connection = await NetworkConnection.create(mockAddress, jest.fn(), jest.fn());

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
      getWebsocket(connection).readyState = 0;

      const promise = connection.sendPayload(mockRequest);

      // check if message has already been sent
      expect(networkMessageHandler).toHaveBeenCalledTimes(0);

      // wait 100 ms
      await new Promise((resolve) => setTimeout(resolve, 100));

      // now mock connection establishment
      getWebsocket(connection).readyState = 1;

      // now the promise should resolve
      await expect(promise).resolves.toBeInstanceOf(ExtendedJsonRpcResponse);
      await expect(promise).resolves.toHaveProperty('response.result', 0);
    });

    // not (yet) tested because the timeout is larger than the timeout for the tests..
    // it('rejects after a timeout', () => {});
  });
});
