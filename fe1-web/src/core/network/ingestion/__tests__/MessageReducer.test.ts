import 'jest-extended';

import { AnyAction } from 'redux';

import {
  configureTestFeatures,
  mockPopToken,
  mockPrivateKey,
  mockPublicKey,
} from '__tests__/utils';
import { Message } from 'core/network/jsonrpc/messages';
import { channelFromIds, KeyPair, Timestamp } from 'core/objects';
import { AddChirp } from 'features/social/network/messages/chirp';

import { ExtendedMessage, markMessageAsProcessed } from '../ExtendedMessage';
import {
  addMessages,
  getMessage,
  makeMessageSelector,
  messageReduce,
  messageReducerPath,
  processMessages,
} from '../MessageReducer';

jest.mock('features/wallet/objects/Token', () => ({
  getCurrentPopTokenFromStore: jest.fn(() => Promise.resolve(mockPopToken)),
}));

const initialState = {
  byId: {},
  allIds: [],
  unprocessedIds: [],
};

const keyPair = KeyPair.fromState({
  privateKey: mockPrivateKey,
  publicKey: mockPublicKey,
});

const createExtendedMessage = () => {
  const messageData = new AddChirp({
    text: 'text',
    timestamp: new Timestamp(1607277600),
  });
  const message = Message.fromData(messageData, keyPair);
  const channel = channelFromIds();
  return ExtendedMessage.fromMessage(message, channel, 'some address');
};

beforeAll(configureTestFeatures);

describe('MessageReducer', () => {
  it('should return the initial state', () => {
    expect(messageReduce(undefined, {} as AnyAction)).toEqual(initialState);
  });

  it('should add the message', async () => {
    const extMsg = createExtendedMessage();
    const msgId = extMsg.message_id.toString();

    const filledState = {
      byId: { [msgId]: extMsg.toState() },
      allIds: [msgId],
      unprocessedIds: [msgId],
    };

    expect(messageReduce(initialState, addMessages(extMsg.toState()))).toEqual(filledState);
  });

  it('should process the message', async () => {
    const extMsg = createExtendedMessage();
    const msgId = extMsg.message_id.toString();

    const filledState = {
      byId: { [msgId]: extMsg.toState() },
      allIds: [msgId],
      unprocessedIds: [msgId],
    };

    const extMsgProcessed = markMessageAsProcessed(extMsg.toState());
    const processedState = {
      byId: { [msgId]: extMsgProcessed },
      allIds: [msgId],
      unprocessedIds: [],
    };

    expect(messageReduce(filledState, processMessages([msgId]))).toEqual(processedState);
  });
});

describe('message selectors', () => {
  it('should return undefined if the message id is not in store', () => {
    const state = {
      byId: {},
      allIds: ['1234'],
      unprocessedIds: ['1234'],
    };
    expect(getMessage(state, '1234')).toEqual(undefined);
  });
});

describe('makeMessageSelector', () => {
  it('should return undefined if the message id is not in store', () => {
    const state = {
      [messageReducerPath]: {
        byId: {},
        allIds: ['1234'],
        unprocessedIds: ['1234'],
      },
    };
    expect(makeMessageSelector('1234')(state)).toEqual(undefined);
  });

  it('should return the message state', () => {
    const extMsg = createExtendedMessage();
    const extMsgState = extMsg.toState();

    const state = {
      [messageReducerPath]: {
        byId: {
          '1234': extMsgState,
        },
        allIds: ['1234'],
        unprocessedIds: ['1234'],
      },
    };
    expect(makeMessageSelector('1234')(state)?.toState()).toEqual(extMsgState);
  });
});
