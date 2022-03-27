import { describe } from '@jest/globals';

import { configureTestFeatures, mockAddress, mockChannel, mockKeyPair } from '__tests__/utils';
import { ExtendedMessage, ExtendedMessageState } from 'core/network/ingestion/ExtendedMessage';
import { ActionType, Message, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { Timestamp } from 'core/objects';

import { addMessageToWitness, witnessMessage, witnessReduce } from '../WitnessReducer';

const timestamp = new Timestamp(1607277600);

beforeAll(() => {
  // we need to set up the message registry for Message.fromData to work
  configureTestFeatures();
});

describe('WitnesssReducer', () => {
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

      const extendedMessageState: ExtendedMessageState = ExtendedMessage.fromMessage(
        message,
        mockChannel,
        mockAddress,
      ).toState();

      const newState = witnessReduce(
        { allIds: [], byId: {} },
        addMessageToWitness(extendedMessageState),
      );

      expect(newState.allIds).toEqual([message.message_id.valueOf()]);
      expect(newState.byId).toHaveProperty(message.message_id.valueOf(), extendedMessageState);
    });
  });

  describe('witnessMessage', () => {
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

      const extendedMessageState: ExtendedMessageState = ExtendedMessage.fromMessage(
        message,
        mockChannel,
        mockAddress,
      ).toState();

      const messageId = message.message_id.valueOf();

      const newState = witnessReduce(
        { allIds: [messageId], byId: { [messageId]: extendedMessageState } },
        witnessMessage(extendedMessageState),
      );

      expect(newState.allIds).toEqual([]);
      expect(newState.byId).toEqual({});
    });
  });
});
