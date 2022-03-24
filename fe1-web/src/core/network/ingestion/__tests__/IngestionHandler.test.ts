import { mockKeyPair } from '__tests__/utils';
import {
  Broadcast,
  ExtendedJsonRpcRequest,
  JsonRpcMethod,
  JsonRpcRequest,
} from 'core/network/jsonrpc';
import {
  ActionType,
  configureMessages,
  Message,
  MessageData,
  MessageRegistry,
  ObjectType,
} from 'core/network/jsonrpc/messages';
import { dispatch } from 'core/redux';

import { ExtendedMessage } from '../ExtendedMessage';
import { handleExtendedRpcRequests } from '../Handler';
import { addMessages } from '../MessageReducer';

// region mocks

jest.mock('core/redux', () => {
  const actualModule = jest.requireActual('core/redux');
  return {
    ...actualModule,
    dispatch: jest.fn().mockImplementation(() => {}),
  };
});

const mockAddress = 'some address';
const mockChannel = 'some channel';
const mockMessageData: MessageData = { object: ObjectType.ELECTION, action: ActionType.OPEN };

// these can only be instantiated after mocking the message registry
let mockMessage: Message;
let mockJsonRequest: Partial<JsonRpcRequest>;
let extendedRequest: ExtendedJsonRpcRequest;

const mockMessageRegistry = {
  getSignatureType: jest.fn().mockImplementation(() => 'some signature'),
  buildMessageData: jest.fn().mockImplementation((input) => JSON.stringify(input)),
} as unknown as MessageRegistry;

// endregion

beforeAll(() => {
  configureMessages(mockMessageRegistry);
  mockMessage = Message.fromData(mockMessageData, mockKeyPair);
  mockJsonRequest = {
    jsonrpc: 'some data',
    method: JsonRpcMethod.BROADCAST,
    params: { channel: mockChannel, message: mockMessage } as Broadcast,
  };
  extendedRequest = new ExtendedJsonRpcRequest({ receivedFrom: mockAddress }, mockJsonRequest);
});

describe('handleExtendedRpcRequests', () => {
  it('dispatches the correct redux action', () => {
    handleExtendedRpcRequests(extendedRequest);
    expect(dispatch).toHaveBeenCalledWith(
      addMessages(ExtendedMessage.fromMessage(mockMessage, mockChannel, mockAddress).toState()),
    );
  });
});
