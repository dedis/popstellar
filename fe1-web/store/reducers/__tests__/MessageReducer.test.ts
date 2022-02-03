import 'jest-extended';
import { AnyAction } from 'redux';
import { channelFromIds, Timestamp } from 'model/objects';
import {
  ExtendedMessage,
  markExtMessageAsProcessed,
  Message,
} from 'model/network/method/message';
import { AddChirp } from 'model/network/method/message/data';
import { mockPopToken } from 'model/network/method/message/__tests__/Message.test';
import {
  addMessages, getLaoMessagesState, getMessage, makeLaoMessagesState,
  messageReduce,
  processMessages,
} from '../MessageReducer';
import { mockLaoId } from './SocialReducer.test';

jest.mock('model/objects/wallet/Token.ts', () => ({
  getCurrentPopTokenFromStore: jest.fn(() => Promise.resolve(mockPopToken)),
}));

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

describe('MessageReducer', () => {
  it('should return the initial state', () => {
    expect(messageReduce(undefined, {} as AnyAction))
      .toEqual(initialState);
  });

  it('should add the message', async () => {
    const messageData = new AddChirp({
      text: 'text',
      timestamp: new Timestamp(1607277600),
    });
    const message = await Message.fromData(messageData);
    const channel = channelFromIds();
    const extMsg = ExtendedMessage.fromMessage(message, channel);
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

    expect(messageReduce(initialState, addMessages(mockLaoId, extMsg.toState())))
      .toEqual(filledState);
  });

  it('should process the message', async () => {
    const messageData = new AddChirp({
      text: 'text',
      timestamp: new Timestamp(1607277600),
    });
    const message = await Message.fromData(messageData);
    const channel = channelFromIds();
    const extMsg = ExtendedMessage.fromMessage(message, channel);
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

    expect(messageReduce(filledState, processMessages(mockLaoId, [msgId])))
      .toEqual(processedState);
  });
});

describe('message selectors', () => {
  it('should return undefined if lao id is undefined', () => {
    expect(makeLaoMessagesState().resultFunc({ [mockLaoId]: randomState }, undefined))
      .toEqual(undefined);
  });

  it('should return empty state', () => {
    expect(getLaoMessagesState(mockLaoId, randomState))
      .toEqual(emptyState);
  });

  it('should return undefined', () => {
    expect(getMessage(randomState, '1234'))
      .toEqual(undefined);
  });
});
