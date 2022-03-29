import 'jest-extended';

import { describe } from '@jest/globals';
import { AnyAction } from 'redux';

import {
  mockLaoCreationTime,
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  mockLaoState,
  org,
} from '__tests__/utils/TestUtils';
import { Hash, Timestamp } from 'core/objects';

import { Lao, LaoState } from '../../objects';
import {
  addLao,
  clearAllLaos,
  connectToLao,
  disconnectFromLao,
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
  };

  rollCallId = new Hash('1234');

  filledState1 = {
    byId: { [mockLaoId]: mockLaoState },
    allIds: [mockLaoId],
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
  }).toState();

  filledStateAfterRollCall = {
    byId: { [mockLaoId]: laoAfterRollCall },
    allIds: [mockLaoId],
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
  };

  filledState2 = {
    byId: {
      [mockLaoId]: mockLaoState,
      [mockLao2Id]: lao2,
    },
    allIds: [mockLaoId, mockLao2Id],
  };

  connectedState1 = {
    byId: { [mockLaoId]: mockLaoState },
    allIds: [mockLaoId],
    currentId: mockLaoId,
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
    expect(laoReduce(emptyState, addLao(mockLaoState))).toEqual(filledState1);
  });

  it('should not add a lao twice', () => {
    expect(laoReduce(filledState1, addLao(mockLaoState))).toEqual(filledState1);
  });

  it('should not update lao if it is not in store', () => {
    expect(laoReduce(filledState1, updateLao(lao2))).toEqual(filledState1);
  });

  it('should update lao if its id is in store', () => {
    expect(laoReduce(filledState1, updateLao(laoUpdated))).toEqual(filledStateUpdated);
  });

  it('should remove a lao', () => {
    expect(laoReduce(filledState1, removeLao(mockLaoIdHash))).toEqual(emptyState);
  });

  it('should not remove a non-stored lao', () => {
    expect(laoReduce(filledState1, removeLao(mockLao2IdHash))).toEqual(filledState1);
  });

  it('should clear all Laos', () => {
    expect(laoReduce(filledState2, clearAllLaos())).toEqual(emptyState);
  });

  it('should connect to lao', () => {
    expect(laoReduce(emptyState, connectToLao(mockLaoState))).toEqual(connectedState1);
  });

  it('should disconnect from lao', () => {
    expect(laoReduce(connectedState1, disconnectFromLao())).toEqual(filledState1);
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
});
