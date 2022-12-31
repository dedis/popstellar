import { AnyAction } from 'redux';

import { mockChannel, serializedMockLaoId } from '__tests__/utils';
import { messageReducerPath, MessageReducerState } from 'core/network/ingestion';

import {
  addUnhandledGreetLaoMessage,
  getUnhandledGreetLaoMessageIds,
  greetLaoReduce,
  GreetLaoReducerState,
  GREET_LAO_REDUCER_PATH,
  selectUnhandledGreetLaoWitnessSignaturesByMessageId,
} from '../GreetLaoReducer';

const emptyState = {
  unhandledIds: [],
} as GreetLaoReducerState;

const mockMessageId1 = 'someMessageId';
const mockMessageId2 = 'someOtherMessageId';

describe('GreetLaoReducer', () => {
  it('should return the initial state', () => {
    expect(greetLaoReduce(undefined, {} as AnyAction)).toEqual(emptyState);
  });

  describe('addUnhandledGreetLaoMessage', () => {
    it('should add a lao#greet message id to the store', () => {
      expect(
        greetLaoReduce(
          {
            unhandledIds: [mockMessageId1],
          } as GreetLaoReducerState,
          addUnhandledGreetLaoMessage({ messageId: mockMessageId2 }),
        ),
      ).toEqual({
        unhandledIds: [mockMessageId1, mockMessageId2],
      } as GreetLaoReducerState);
    });

    it('should not add a lao#greet message id twice to the store', () => {
      expect(
        greetLaoReduce(
          {
            unhandledIds: [mockMessageId1],
          } as GreetLaoReducerState,
          addUnhandledGreetLaoMessage({ messageId: mockMessageId1 }),
        ),
      ).toEqual({
        unhandledIds: [mockMessageId1],
      } as GreetLaoReducerState);
    });
  });

  describe('getGreetLaoMessageIdsByLao', () => {
    it('should return the correct value', () => {
      expect(
        getUnhandledGreetLaoMessageIds({
          [GREET_LAO_REDUCER_PATH]: {
            unhandledIds: [mockMessageId1, mockMessageId2],
          } as GreetLaoReducerState,
        }),
      ).toEqual([mockMessageId1, mockMessageId2]);
    });
  });

  describe('selectUnhandledGreetLaoWitnessSignaturesByMessageId', () => {
    it('should return the correct value', () => {
      expect(
        selectUnhandledGreetLaoWitnessSignaturesByMessageId({
          [GREET_LAO_REDUCER_PATH]: {
            unhandledIds: [mockMessageId1, mockMessageId2],
          } as GreetLaoReducerState,
          [messageReducerPath]: {
            allIds: [mockMessageId1, mockMessageId2],
            unprocessedIds: [],
            byId: {
              [mockMessageId1]: {
                message_id: mockMessageId1,
                data: '',
                laoId: serializedMockLaoId,
                receivedAt: 0,
                receivedFrom: '',
                sender: '',
                signature: '',
                witness_signatures: [{ signature: 'mockSignature', witness: 'mockWitness' }],
                channel: mockChannel,
              },
              [mockMessageId2]: {
                message_id: mockMessageId2,
                data: '',
                laoId: serializedMockLaoId,
                receivedAt: 0,
                receivedFrom: '',
                sender: '',
                signature: '',
                witness_signatures: [{ signature: 'mockSignature2', witness: 'mockWitness2' }],
                channel: mockChannel,
              },
            },
          } as MessageReducerState,
        }),
      ).toEqual({
        [mockMessageId1]: [{ signature: 'mockSignature', witness: 'mockWitness' }],
        [mockMessageId2]: [{ signature: 'mockSignature2', witness: 'mockWitness2' }],
      });
    });
  });
});
