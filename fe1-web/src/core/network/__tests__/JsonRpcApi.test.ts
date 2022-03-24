/* eslint-disable @typescript-eslint/dot-notation */
import 'jest-extended';
import '__tests__/utils/matchers';
import { describe, jest } from '@jest/globals';

import { mockKeyPair } from '__tests__/utils';
import { KeyPairRegistry } from 'core/keypair';
import { Channel } from 'core/objects';

import { ExtendedMessage } from '../ingestion/ExtendedMessage';
import {
  ExtendedJsonRpcResponse,
  JsonRpcMethod,
  JsonRpcRequest,
  JsonRpcResponse,
  Publish,
  Subscribe,
} from '../jsonrpc';
import {
  ActionType,
  configureMessages,
  Message,
  MessageData,
  MessageRegistry,
  ObjectType,
} from '../jsonrpc/messages';
import {
  AUTO_ASSIGN_ID,
  catchup,
  configureJsonRpcApi,
  getSigningKeyPair,
  publish,
  subscribe,
} from '../JsonRpcApi';
import { getNetworkManager } from '../NetworkManager';

// region mock data

const mockSignatureType = 'some signature';

const mockMessageRegistry = {
  getSignatureType: jest.fn().mockImplementation(() => mockSignatureType),
  buildMessageData: jest.fn().mockImplementation((input) => JSON.stringify(input)),
} as unknown as MessageRegistry;

const mockKeyPairRegistry = {
  getSignatureKeyPair: jest.fn().mockImplementation(() => Promise.resolve(mockKeyPair)),
} as unknown as KeyPairRegistry;

const mockChannel: Channel = 'some channel';
const mockAddress = 'some address';
const mockMessageData: MessageData = { object: ObjectType.ELECTION, action: ActionType.OPEN };

const networkManager = getNetworkManager();

// requires the mock registries to be set up
// is initialized in beforeAll()
let mockResponseMessage: Message;
let mockResponse: JsonRpcResponse;

const mockSendingStrategy = jest
  .fn()
  .mockImplementation(() =>
    Promise.resolve([new ExtendedJsonRpcResponse({ receivedFrom: mockAddress }, mockResponse)]),
  );

// endregion

beforeAll(() => {
  // set up mock registries
  configureJsonRpcApi(mockMessageRegistry, mockKeyPairRegistry);
  configureMessages(mockMessageRegistry);
  // replace sending strategy of network manager with a jest mock function

  // @ts-ignore
  networkManager['sendingStrategy'] = mockSendingStrategy;

  // this cannot be initialized before as it requires the mock registries to be set up
  mockResponseMessage = Message.fromData(mockMessageData, mockKeyPair, []);
  mockResponse = {
    id: 0,
    result: [mockResponseMessage],
  };
});

afterEach(() => {
  jest.clearAllMocks();
});

describe('getSigningKeyPair', () => {
  it('calls the correct functions with the correct aguments', async () => {
    expect(await getSigningKeyPair(mockMessageData)).toBe(mockKeyPair);

    expect(mockMessageRegistry.getSignatureType).toHaveBeenCalledWith(mockMessageData);
    expect(mockMessageRegistry.getSignatureType).toHaveBeenCalledTimes(1);

    expect(mockKeyPairRegistry.getSignatureKeyPair).toHaveBeenCalledWith(mockSignatureType);
    expect(mockKeyPairRegistry.getSignatureKeyPair).toHaveBeenCalledTimes(1);
  });
});

describe('publish', () => {
  it('correctly builds a JsonRpcRequest and passes it to the network manager', async () => {
    await publish(mockChannel, mockMessageData);

    const message = await Message.fromData(mockMessageData, mockKeyPair);
    const request = new JsonRpcRequest({
      method: JsonRpcMethod.PUBLISH,
      params: new Publish({
        channel: mockChannel,
        message: message,
      }),
      id: AUTO_ASSIGN_ID,
    });

    expect(mockSendingStrategy).toHaveBeenCalledWith(request, expect.anything());
    expect(mockSendingStrategy).toHaveBeenCalledTimes(1);
  });
});

describe('subscribe', () => {
  it('correctly builds a JsonRpcRequest and passes it to the network manager', async () => {
    await subscribe(mockChannel);

    const request = new JsonRpcRequest({
      method: JsonRpcMethod.SUBSCRIBE,
      params: new Subscribe({
        channel: mockChannel,
      }),
      id: AUTO_ASSIGN_ID,
    });

    expect(mockSendingStrategy).toHaveBeenCalledWith(request, expect.anything());
    expect(mockSendingStrategy).toHaveBeenCalledTimes(1);
  });
});

describe('catchup', () => {
  it('correctly builds a JsonRpcRequest and passes it to the network manager', async () => {
    await catchup(mockChannel);

    const request = new JsonRpcRequest({
      method: JsonRpcMethod.CATCHUP,
      params: new Subscribe({
        channel: mockChannel,
      }),
      id: AUTO_ASSIGN_ID,
    });

    expect(mockSendingStrategy).toHaveBeenCalledWith(request, expect.anything());
    expect(mockSendingStrategy).toHaveBeenCalledTimes(1);
  });

  it('returns the a valid message generator', async () => {
    const generator = await catchup(mockChannel);
    const { value, done } = generator.next();

    expect(value).toBeJsonEqual(
      ExtendedMessage.fromMessage(mockResponseMessage, mockChannel, mockAddress),
    );
    expect(done).toBeFalse();

    expect(generator.next().done).toBeTrue();
  });
});
