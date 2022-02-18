import 'jest-extended';
import { AnyAction } from 'redux';

import { channelFromIds, Timestamp } from 'core/objects';
import { MessageRegistry, Message, configureMessages } from 'core/network/jsonrpc/messages';
import { mockLaoId, mockPopToken } from '__tests__/utils/TestUtils';

import { AddChirp } from 'features/social/network/messages/chirp';

import {
  addMessages,
  getLaoMessagesState,
  getMessage,
  makeLaoMessagesState,
  messageReduce,
  processMessages,
} from '../Reducer';
import { ExtendedMessage } from '../ExtendedMessage';

jest.mock('features/wallet/objects/Token.ts', () => ({
  getCurrentPopTokenFromStore: jest.fn(() => Promise.resolve(mockPopToken)),
}));

const messageRegistry = new MessageRegistry();
configureMessages(messageRegistry);

const initialState = {
  byLaoId: {},
};

const randomState = {
  byId: {},
  allIds: ['1234'],
  unprocessedIds: ['1234'],
};

const emptyState = {
  byId: {},
  allIds: [],
  unprocessedIds: [],
};

const createExtendedMessage = async () => {
  const messageData = new AddChirp({
    text: 'text',
    timestamp: new Timestamp(1607277600),
  });
  const message = await Message.fromData(messageData);
  const channel = channelFromIds();
  return ExtendedMessage.fromMessage(message, channel);
};

describe('MessageReducer', () => {
  it('should return the initial state', () => {
    expect(messageReduce(undefined, {} as AnyAction)).toEqual(initialState);
  });

  it('should add the message', async () => {
    const extMsg = await createExtendedMessage();
    const msgId = extMsg.message_id.toString();

    const filledState = {
      byLaoId: {
        [mockLaoId]: {
          byId: { [msgId]: extMsg.toState() },
          allIds: [msgId],
          unprocessedIds: [msgId],
        },
      },
    };

    expect(messageReduce(initialState, addMessages(mockLaoId, extMsg.toState()))).toEqual(
      filledState,
    );
  });

  it('should process the message', async () => {
    const extMsg = await createExtendedMessage();
    const msgId = extMsg.message_id.toString();

    const filledState = {
      byLaoId: {
        [mockLaoId]: {
          byId: { [msgId]: extMsg.toState() },
          allIds: [msgId],
          unprocessedIds: [msgId],
        },
      },
    };

    const extMsgProcessed = markExtMessageAsProcessed(extMsg.toState());
    const processedState = {
      byLaoId: {
        [mockLaoId]: {
          byId: { [msgId]: extMsgProcessed },
          allIds: [msgId],
          unprocessedIds: [],
        },
      },
    };

    expect(messageReduce(filledState, processMessages(mockLaoId, [msgId]))).toEqual(processedState);
  });
});

describe('message selectors', () => {
  it('should return undefined if lao id is undefined', () => {
    expect(makeLaoMessagesState().resultFunc({ [mockLaoId]: randomState }, undefined)).toEqual(
      undefined,
    );
  });

  it('should return empty state when byId is empty', () => {
    expect(getLaoMessagesState(mockLaoId, randomState)).toEqual(emptyState);
  });

  it('should return undefined if the message id is not in store', () => {
    expect(getMessage(randomState, '1234')).toEqual(undefined);
  });
});
