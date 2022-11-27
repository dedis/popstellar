import 'jest-extended';

import { describe } from '@jest/globals';
import { AnyAction } from 'redux';
import { RehydrateAction } from 'redux-persist';

import {
  mockLaoCreationTime,
  mockLao,
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  mockLaoState,
  org,
  mockAddress,
  mockChannel,
} from '__tests__/utils/TestUtils';
import { channelFromIds, Hash, Timestamp } from 'core/objects';

import { Lao, LaoState } from '../../objects';
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
  selectConnectedToLao,
} from '../LaoReducer';

let emptyState: any;
let filledState1: any;
let filledStateAfterRollCall: any;
let filledStateUpdated: any;
let filledState2: any;
let connectedState1: any;
let laoRecord: any;

let laoAfterRollCall: LaoState;
let lao2: LaoState;
let laoUpdated: LaoState;

let rollCallId: Hash;
let mockLao2IdHash: Hash;

const initializeData = () => {
  emptyState = {
    byId: {},
    allIds: [],
    connected: false,
  };

  rollCallId = new Hash('1234');

  filledState1 = {
    byId: { [mockLaoId]: mockLaoState },
    allIds: [mockLaoId],
    connected: false,
  };

  laoAfterRollCall = new Lao({
    id: mockLaoIdHash,
    name: mockLaoName,
    creation: mockLaoCreationTime,
    last_modified: mockLaoCreationTime,
    organizer: org,
    witnesses: [],
    last_roll_call_id: rollCallId,
    last_tokenized_roll_call_id: rollCallId,
    server_addresses: [],
    subscribed_channels: [channelFromIds(mockLaoIdHash)],
  }).toState();

  filledStateAfterRollCall = {
    byId: { [mockLaoId]: laoAfterRollCall },
    allIds: [mockLaoId],
    connected: false,
  };

  const mockLao2Name = 'Second Lao';
  mockLao2IdHash = Hash.fromStringArray(
    org.toString(),
    mockLaoCreationTime.toString(),
    mockLao2Name,
  );
  const mockLao2Id = mockLao2IdHash.toString();

  lao2 = new Lao({
    id: mockLao2IdHash,
    name: mockLao2Name,
    creation: mockLaoCreationTime,
    last_modified: mockLaoCreationTime,
    organizer: org,
    witnesses: [],
  }).toState();

  laoUpdated = new Lao({
    id: mockLaoIdHash,
    name: mockLaoName,
    creation: mockLaoCreationTime,
    last_modified: new Timestamp(1606666600),
    organizer: org,
    witnesses: [],
  }).toState();

  filledStateUpdated = {
    byId: { [mockLaoId]: laoUpdated },
    allIds: [mockLaoId],
    connected: false,
  };

  filledState2 = {
    byId: {
      [mockLaoId]: mockLaoState,
      [mockLao2Id]: lao2,
    },
    allIds: [mockLaoId, mockLao2Id],
    connected: false,
  };

  connectedState1 = {
    byId: { [mockLaoId]: mockLaoState },
    allIds: [mockLaoId],
    currentId: mockLaoId,
    connected: true,
  };

  laoRecord = { [mockLaoId]: mockLaoState };
};

beforeEach(() => {
  initializeData();
});

describe('LaoReducer', () => {
  it('should return the initial state', () => {
    expect(laoReduce(undefined, {} as AnyAction)).toEqual(emptyState);
  });

  it('should add lao', () => {
    expect(laoReduce(emptyState, addLao({ lao: mockLaoState }))).toEqual(filledState1);
  });

  it('should not add a lao twice', () => {
    expect(laoReduce(filledState1, addLao({ lao: mockLaoState }))).toEqual(filledState1);
  });

  it('should not update lao if it is not in store', () => {
    expect(laoReduce(filledState1, updateLao({ lao: lao2 }))).toEqual(filledState1);
  });

  it('should update lao if its id is in store', () => {
    expect(laoReduce(filledState1, updateLao({ lao: laoUpdated }))).toEqual(filledStateUpdated);
  });

  it('should remove a lao', () => {
    expect(laoReduce(filledState1, removeLao({ laoId: mockLaoIdHash }))).toEqual(emptyState);
  });

  it('should not remove a non-stored lao', () => {
    expect(laoReduce(filledState1, removeLao({ laoId: mockLao2IdHash }))).toEqual(filledState1);
  });

  it('should clear all Laos', () => {
    expect(laoReduce(filledState2, clearAllLaos())).toEqual(emptyState);
  });

  it('should connect to lao', () => {
    expect(laoReduce(emptyState, setCurrentLao({ lao: mockLaoState }))).toEqual(connectedState1);
  });

  it('should allow entering offline mode', () => {
    expect(laoReduce(emptyState, setCurrentLao({ lao: mockLaoState, connected: false }))).toEqual({
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
          allIds: [mockLaoId],
          byId: {
            [mockLaoId]: { ...mockLaoState, server_addresses: [] },
          },
          connected: false,
        } as LaoReducerState,
        addLaoServerAddress(mockLaoId, mockAddress),
      ),
    ).toEqual({
      allIds: [mockLaoId],
      byId: {
        [mockLaoId]: { ...mockLaoState, server_addresses: [mockAddress] },
      },
      connected: false,
    } as LaoReducerState);
  });
});

it('should add subscribed channels', () => {
  expect(
    laoReduce(
      {
        allIds: [mockLaoId],
        byId: {
          [mockLaoId]: { ...mockLaoState, subscribed_channels: [] },
        },
        connected: false,
      } as LaoReducerState,
      addSubscribedChannel(mockLaoId, mockChannel),
    ),
  ).toEqual({
    allIds: [mockLaoId],
    byId: {
      [mockLaoId]: { ...mockLaoState, subscribed_channels: [mockChannel] },
    },
    connected: false,
  } as LaoReducerState);
});

it('should remove subscribed channels', () => {
  expect(
    laoReduce(
      {
        allIds: [mockLaoId],
        byId: {
          [mockLaoId]: { ...mockLaoState, subscribed_channels: [mockChannel] },
        },
        connected: false,
      } as LaoReducerState,
      removeSubscribedChannel(mockLaoId, mockChannel),
    ),
  ).toEqual({
    allIds: [mockLaoId],
    byId: {
      [mockLaoId]: { ...mockLaoState, subscribed_channels: [] },
    },
    connected: false,
  } as LaoReducerState);
});

describe('Lao selector', () => {
  it('should return undefined for makeLao when the currentId is undefined', () => {
    expect(makeLao().resultFunc(laoRecord, undefined)).toEqual(undefined);
  });

  it('should return lao for selectCurrentLao', () => {
    expect(selectCurrentLao.resultFunc(laoRecord, mockLaoId)).toEqual(Lao.fromState(mockLaoState));
  });

  it('should return an empty makeLaoIdsList when there is no lao', () => {
    expect(selectLaoIdsList.resultFunc([])).toEqual([]);
  });

  it('should return makeLaosList correctly', () => {
    expect(selectLaosList.resultFunc(laoRecord, [mockLaoId])).toEqual([
      Lao.fromState(mockLaoState),
    ]);
  });

  it('should return makeLaosMap correctly', () => {
    expect(selectLaosMap.resultFunc(laoRecord)).toEqual({
      [mockLaoId]: Lao.fromState(mockLaoState),
    });
  });

  it('should return true for makeIsLaoOrganizer when it is true', () => {
    expect(selectIsLaoOrganizer.resultFunc(laoRecord, mockLaoId, org.toString())).toEqual(true);
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
              [mockLaoId]: mockLao.toState(),
            },
            allIds: [mockLaoId],
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
              [mockLaoId]: mockLao.toState(),
            },
            allIds: [mockLaoId],
            currentId: mockLaoId,
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
              [mockLaoId]: mockLao.toState(),
            },
            allIds: [mockLaoId],
            currentId: mockLaoId,
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
