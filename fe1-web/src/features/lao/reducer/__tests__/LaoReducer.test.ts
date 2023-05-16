import 'jest-extended';

import { describe } from '@jest/globals';
import { AnyAction } from 'redux';
import { RehydrateAction } from 'redux-persist';

import {
  mockLaoCreationTime,
  mockLao,
  serializedMockLaoId,
  mockLaoId,
  mockLaoName,
  mockLaoState,
  org,
  mockAddress,
  mockChannel,
} from '__tests__/utils/TestUtils';
import { channelFromIds, Hash, Timestamp } from 'core/objects';

import { Lao } from '../../objects';
import {
  addLao,
  clearAllLaos,
  setCurrentLao,
  clearCurrentLao,
  laoReduce,
  selectIsLaoOrganizer,
  makeLao,
  selectLaoIdsList,
  selectLaosList,
  selectLaosMap,
  removeLao,
  selectCurrentLao,
  setLaoLastRollCall,
  updateLao,
  LAO_REDUCER_PATH,
  getLaoById,
  LaoReducerState,
  addLaoServerAddress,
  addSubscribedChannel,
  removeSubscribedChannel,
  selectConnectedToLao, reconnectLao
} from '../LaoReducer';

const rollCallId = new Hash('1234');

const laoAfterRollCall = new Lao({
  id: mockLaoId,
  name: mockLaoName,
  creation: mockLaoCreationTime,
  last_modified: mockLaoCreationTime,
  organizer: org,
  witnesses: [],
  last_roll_call_id: rollCallId,
  last_tokenized_roll_call_id: rollCallId,
  server_addresses: [],
  subscribed_channels: [channelFromIds(mockLaoId)],
});

const laoAfterRollCallState = laoAfterRollCall.toState();

const mockLao2Name = 'Second Lao';
const mockLao2Id = Hash.fromArray(org, mockLaoCreationTime, mockLao2Name);
const serializedMockLao2Id = mockLao2Id.toString();

const lao2 = new Lao({
  id: mockLao2Id,
  name: mockLao2Name,
  creation: mockLaoCreationTime,
  last_modified: mockLaoCreationTime,
  organizer: org,
  witnesses: [],
});

const laoUpdated = new Lao({
  id: mockLaoId,
  name: mockLaoName,
  creation: mockLaoCreationTime,
  last_modified: new Timestamp(1606666600),
  organizer: org,
  witnesses: [],
});

const emptyState: LaoReducerState = {
  byId: {},
  allIds: [],
  connected: false,
};

const filledState1: LaoReducerState = {
  byId: { [serializedMockLaoId]: mockLaoState },
  allIds: [serializedMockLaoId],
  connected: false,
};

const filledStateAfterRollCall = {
  byId: { [serializedMockLaoId]: laoAfterRollCallState },
  allIds: [serializedMockLaoId],
  connected: false,
};

const filledStateUpdated: LaoReducerState = {
  byId: { [serializedMockLaoId]: laoUpdated.toState() },
  allIds: [serializedMockLaoId],
  connected: false,
};

const filledState2: LaoReducerState = {
  byId: {
    [serializedMockLaoId]: mockLaoState,
    [serializedMockLao2Id]: lao2.toState(),
  },
  allIds: [serializedMockLaoId, serializedMockLao2Id],
  connected: false,
};

const disconnectedState1: LaoReducerState = {
  byId: { [serializedMockLaoId]: mockLaoState },
  allIds: [serializedMockLaoId],
  currentId: serializedMockLaoId,
  connected: true,
};

const connectedState1: LaoReducerState = {
  byId: { [serializedMockLaoId]: mockLaoState },
  allIds: [serializedMockLaoId],
  currentId: serializedMockLaoId,
  connected: true,
};

const laoRecord = { [serializedMockLaoId]: mockLaoState };

describe('LaoReducer', () => {
  it('should return the initial state', () => {
    expect(laoReduce(undefined, {} as AnyAction)).toEqual(emptyState);
  });

  it('should add lao', () => {
    expect(laoReduce(emptyState, addLao(mockLao))).toEqual(filledState1);
  });

  it('should not add a lao twice', () => {
    expect(laoReduce(filledState1, addLao(mockLao))).toEqual(filledState1);
  });

  it('should not update lao if it is not in store', () => {
    expect(laoReduce(filledState1, updateLao(lao2.id, lao2.toState()))).toEqual(filledState1);
  });

  it('should update lao if its id is in store', () => {
    expect(laoReduce(filledState1, updateLao(laoUpdated.id, laoUpdated.toState()))).toEqual(
      filledStateUpdated,
    );
  });

  it('should remove a lao', () => {
    expect(laoReduce(filledState1, removeLao(mockLaoId))).toEqual(emptyState);
  });

  it('should not remove a non-stored lao', () => {
    expect(laoReduce(filledState1, removeLao(mockLao2Id))).toEqual(filledState1);
  });

  it('should clear all Laos', () => {
    expect(laoReduce(filledState2, clearAllLaos())).toEqual(emptyState);
  });

  it('should connect to lao', () => {
    expect(laoReduce(emptyState, setCurrentLao(mockLao))).toEqual(connectedState1);
  });

  it('should allow entering offline mode', () => {
    expect(laoReduce(emptyState, setCurrentLao(mockLao, false))).toEqual({
      ...connectedState1,
      connected: false,
    });
  });

  it('should disconnect from lao', () => {
    expect(laoReduce(connectedState1, clearCurrentLao())).toEqual(filledState1);
  });

  it('set last roll call for a non-stored lao does not do anything', () => {
    expect(laoReduce(emptyState, setLaoLastRollCall(mockLaoId, rollCallId, false))).toEqual(
      emptyState,
    );
  });

  it('set last roll call for lao', () => {
    expect(laoReduce(filledState1, setLaoLastRollCall(mockLaoId, rollCallId, true))).toEqual(
      filledStateAfterRollCall,
    );
  });

  it('reconnect to Lao should update connected state', () => {
    expect(laoReduce(disconnectedState1, reconnectLao(mockLaoId))).toEqual(connectedState1);
  });

  it('should not reconnect to Lao if it is not in store', () => {
    expect(laoReduce(disconnectedState1, reconnectLao(mockLao2Id))).toEqual(disconnectedState1);
  });

  it('should clear currentId on rehydration', () => {
    expect(
      laoReduce(connectedState1, {
        type: 'persist/REHYDRATE',
        key: 'root:persist',
        payload: { [LAO_REDUCER_PATH]: {} },
      } as RehydrateAction),
    ).toEqual({ ...connectedState1, currentId: undefined });
  });

  it('should add server addresses', () => {
    expect(
      laoReduce(
        {
          allIds: [serializedMockLaoId],
          byId: {
            [serializedMockLaoId]: { ...mockLaoState, server_addresses: [] },
          },
          connected: false,
        } as LaoReducerState,
        addLaoServerAddress(mockLaoId, mockAddress),
      ),
    ).toEqual({
      allIds: [serializedMockLaoId],
      byId: {
        [serializedMockLaoId]: { ...mockLaoState, server_addresses: [mockAddress] },
      },
      connected: false,
    } as LaoReducerState);
  });
});

it('should add subscribed channels', () => {
  expect(
    laoReduce(
      {
        allIds: [serializedMockLaoId],
        byId: {
          [serializedMockLaoId]: { ...mockLaoState, subscribed_channels: [] },
        },
        connected: false,
      } as LaoReducerState,
      addSubscribedChannel(mockLaoId, mockChannel),
    ),
  ).toEqual({
    allIds: [serializedMockLaoId],
    byId: {
      [serializedMockLaoId]: { ...mockLaoState, subscribed_channels: [mockChannel] },
    },
    connected: false,
  } as LaoReducerState);
});

it('should remove subscribed channels', () => {
  expect(
    laoReduce(
      {
        allIds: [serializedMockLaoId],
        byId: {
          [serializedMockLaoId]: { ...mockLaoState, subscribed_channels: [mockChannel] },
        },
        connected: false,
      } as LaoReducerState,
      removeSubscribedChannel(mockLaoId, mockChannel),
    ),
  ).toEqual({
    allIds: [serializedMockLaoId],
    byId: {
      [serializedMockLaoId]: { ...mockLaoState, subscribed_channels: [] },
    },
    connected: false,
  } as LaoReducerState);
});

describe('Lao selector', () => {
  it('should return undefined for makeLao when the currentId is undefined', () => {
    expect(makeLao().resultFunc(laoRecord, undefined)).toEqual(undefined);
  });

  it('should return lao for selectCurrentLao', () => {
    expect(selectCurrentLao.resultFunc(laoRecord, serializedMockLaoId)).toEqual(
      Lao.fromState(mockLaoState),
    );
  });

  it('should return an empty makeLaoIdsList when there is no lao', () => {
    expect(selectLaoIdsList.resultFunc([])).toEqual([]);
  });

  it('should return makeLaosList correctly', () => {
    expect(selectLaosList.resultFunc(laoRecord, [serializedMockLaoId])).toEqual([
      Lao.fromState(mockLaoState),
    ]);
  });

  it('should return makeLaosMap correctly', () => {
    expect(selectLaosMap.resultFunc(laoRecord)).toEqual({
      [serializedMockLaoId]: Lao.fromState(mockLaoState),
    });
  });

  it('should return true for makeIsLaoOrganizer when it is true', () => {
    expect(selectIsLaoOrganizer.resultFunc(laoRecord, serializedMockLaoId, org.toString())).toEqual(
      true,
    );
  });

  describe('getLaoById', () => {
    it('should return undefined if there is no lao with this id', () => {
      expect(
        getLaoById(mockLaoId, {
          [LAO_REDUCER_PATH]: {
            byId: {},
            allIds: [],
            currentId: undefined,
            connected: false,
          } as LaoReducerState,
        }),
      ).toBeUndefined();
    });

    it('should return the correct value if there is a lao with this id', () => {
      expect(
        getLaoById(mockLaoId, {
          [LAO_REDUCER_PATH]: {
            byId: {
              [serializedMockLaoId]: mockLao.toState(),
            },
            allIds: [serializedMockLaoId],
            currentId: undefined,
          } as LaoReducerState,
        }),
      ).toEqual(mockLao);
    });
  });

  describe('selectIsConnected', () => {
    it('returns true if currently connected to a lao', () => {
      expect(
        selectConnectedToLao({
          [LAO_REDUCER_PATH]: {
            byId: {
              [serializedMockLaoId]: mockLao.toState(),
            },
            allIds: [serializedMockLaoId],
            currentId: serializedMockLaoId,
            connected: true,
          } as LaoReducerState,
        }),
      ).toBeTrue();
    });

    it('returns false if there is a current lao but no connection was established (offline mode)', () => {
      expect(
        selectConnectedToLao({
          [LAO_REDUCER_PATH]: {
            byId: {
              [serializedMockLaoId]: mockLao.toState(),
            },
            allIds: [serializedMockLaoId],
            currentId: serializedMockLaoId,
            connected: false,
          } as LaoReducerState,
        }),
      ).toBeFalse();
    });

    it('returns undefined if there is no current lao', () => {
      expect(
        selectConnectedToLao({
          [LAO_REDUCER_PATH]: {
            byId: {},
            allIds: [],
            currentId: undefined,
            connected: false,
          } as LaoReducerState,
        }),
      ).toBeUndefined();
    });
  });
});
