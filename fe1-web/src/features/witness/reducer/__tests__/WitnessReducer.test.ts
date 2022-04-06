import 'jest-extended';
import '__tests__/utils/matchers';
import { describe } from '@jest/globals';

import { configureTestFeatures, mockKeyPair } from '__tests__/utils';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { ActionType, Message, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { Timestamp } from 'core/objects';

import {
  addMessageToWitness,
  getMessageToWitness,
  makeMessageToWitnessSelector,
  removeMessageToWitness,
  witnessReduce,
  WITNESS_REDUCER_PATH,
} from '../WitnessReducer';

const timestamp = new Timestamp(1607277600);

beforeAll(() => {
  // we need to set up the message registry for Message.fromData to work
  configureTestFeatures();
});

describe('WitnessReducer', () => {
  describe('addMessageToWitness', () => {
    it('adds messages to the store', () => {
      const message: Message = Message.fromData(
        {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: 'hi',
          timestamp,
        } as MessageData,
        mockKeyPair,
      );

      const extendedMessage = ExtendedMessage.fromMessage(
        message,
        'some channel',
        'some address',
      ).toState();

      const newState = witnessReduce(
        { allIds: [], byId: {} },
        addMessageToWitness(extendedMessage),
      );

      expect(newState.allIds).toEqual([message.message_id.valueOf()]);
      expect(newState.byId).toHaveProperty(message.message_id.valueOf(), extendedMessage);
    });

    it("doesn't add a messages a second time to the store", () => {
      const message: Message = Message.fromData(
        {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: 'hi',
          timestamp,
        } as MessageData,
        mockKeyPair,
      );

      const extendedMessage = ExtendedMessage.fromMessage(
        message,
        'some channel',
        'some address',
      ).toState();

      const newState = witnessReduce(
        witnessReduce({ allIds: [], byId: {} }, addMessageToWitness(extendedMessage)),
        addMessageToWitness(extendedMessage),
      );

      expect(newState.allIds).toEqual([message.message_id.valueOf()]);
      expect(newState.byId).toHaveProperty(message.message_id.valueOf(), extendedMessage);
    });
  });

  describe('removeMessageToWitness', () => {
    it('removes the witnesses message from the store', () => {
      const message: Message = Message.fromData(
        {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: 'hi',
          timestamp,
        } as MessageData,
        mockKeyPair,
      );

      const messageId = message.message_id.valueOf();

      const newState = witnessReduce(
        {
          allIds: [messageId],
          byId: {
            [messageId]: ExtendedMessage.fromMessage(
              message,
              'some channel',
              'some address',
            ).toState(),
          },
        },
        removeMessageToWitness(message.message_id.valueOf()),
      );

      expect(newState.allIds).toEqual([]);
      expect(newState.byId).toEqual({});
    });

    it("doesn't do anything of the id is not in the store", () => {
      const message: Message = Message.fromData(
        {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: 'hi',
          timestamp,
        } as MessageData,
        mockKeyPair,
      );

      const messageId = message.message_id.valueOf();
      const extMessage = ExtendedMessage.fromMessage(
        message,
        'some channel',
        'some address',
      ).toState();

      const newState = witnessReduce(
        {
          allIds: [messageId],
          byId: {
            [messageId]: extMessage,
          },
        },
        removeMessageToWitness('some other id'),
      );

      expect(newState.allIds).toEqual([messageId]);
      expect(newState.byId).toEqual({
        [messageId]: extMessage,
      });
    });
  });

  describe('getMessageToWitness', () => {
    it('returns the correct message', () => {
      const message: Message = Message.fromData(
        {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: 'hi',
          timestamp,
        } as MessageData,
        mockKeyPair,
      );

      const messageId = message.message_id.valueOf();
      const extMessage = ExtendedMessage.fromMessage(message, 'some channel', 'some address');

      expect(
        getMessageToWitness(messageId, {
          [WITNESS_REDUCER_PATH]: {
            allIds: [messageId],
            byId: {
              [messageId]: extMessage.toState(),
            },
          },
        }),
      ).toBeJsonEqual(extMessage);
    });
  });

  describe('makeMessageToWitnessSelector', () => {
    it('returns the correct message', () => {
      const message: Message = Message.fromData(
        {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: 'hi',
          timestamp,
        } as MessageData,
        mockKeyPair,
      );

      const messageId = message.message_id.valueOf();
      const extMessage = ExtendedMessage.fromMessage(message, 'some channel', 'some address');

      expect(
        makeMessageToWitnessSelector(messageId)({
          [WITNESS_REDUCER_PATH]: {
            allIds: [messageId],
            byId: {
              [messageId]: extMessage.toState(),
            },
          },
        }),
      ).toBeJsonEqual(extMessage);
    });
  });
});
