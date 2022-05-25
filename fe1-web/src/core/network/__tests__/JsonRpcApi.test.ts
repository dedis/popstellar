import 'jest-extended';
import '__tests__/utils/matchers';
import { describe } from '@jest/globals';

import {
  mockAddress,
  mockChannel,
  mockKeyPair,
  mockKeyPairRegistry,
  mockMessageRegistry,
  mockSignatureType,
} from '__tests__/utils';

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
import { SendingStrategy } from '../strategies/ClientMultipleServerStrategy';

// region mock data

// requires the mock registries to be set up
// is initialized in beforeAll()
let mockResponseMessage: Message;
let mockResponse: JsonRpcResponse;

const mockMessageData: MessageData = { object: ObjectType.ELECTION, action: ActionType.OPEN };
const mockSendingStrategy: SendingStrategy = jest.fn(() =>
  Promise.resolve([new ExtendedJsonRpcResponse({ receivedFrom: mockAddress }, mockResponse)]),
);

jest.mock('core/network/NetworkManager', () => {
  const actual = jest.requireActual('/core/network/NetworkManager');
  return {
    ...actual,
    getNetworkManager: () => new actual.TEST_ONLY_EXPORTS.NetworkManager(mockSendingStrategy),
  };
});

// endregion

beforeAll(() => {
  // set up mock registries
  configureJsonRpcApi(mockMessageRegistry, mockKeyPairRegistry);
  configureMessages(mockMessageRegistry);

  // this cannot be initialized before as it requires the mock registries to be set up
  mockResponseMessage = Message.fromData(mockMessageData, mockKeyPair, mockChannel, []);
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

    const message = Message.fromData(mockMessageData, mockKeyPair, mockChannel);
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

  it('returns the valid message generator', async () => {
    const generator = await catchup(mockChannel);
    const { value, done } = generator.next();

    if (!(value instanceof ExtendedMessage)) {
      throw new Error('The generator should contain at least one message');
    }

    const expected = ExtendedMessage.fromMessage(mockResponseMessage, mockAddress, mockChannel);

    // the receivedAt value is allowed differ
    expect({ ...value, receivedAt: 0 }).toBeJsonEqual({ ...expected, receivedAt: 0 });
    expect(done).toBeFalse();

    expect(generator.next().done).toBeTrue();
  });
});
