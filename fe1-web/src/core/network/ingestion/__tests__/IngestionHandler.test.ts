import { mockKeyPair, mockMessageRegistry } from '__tests__/utils';
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
    const obj = addMessages(
      ExtendedMessage.fromMessage(mockMessage, mockChannel, mockAddress).toState(),
    );

    // the receivedAt value can differ. calls[i][j] => jth argument of ith call
    // @ts-ignore
    delete obj.payload[0].receivedAt;
    delete (dispatch as jest.Mock).mock.calls[0][0].payload[0].receivedAt;

    expect(dispatch).toHaveBeenCalledWith(obj);
  });
});
