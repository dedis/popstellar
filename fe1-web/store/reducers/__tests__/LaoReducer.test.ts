import 'jest-extended';
import { describe } from '@jest/globals';
import { AnyAction } from 'redux';
import { Hash, Lao, Timestamp } from 'model/objects';
import {
  addLao,
  clearAllLaos,
  connectToLao,
  disconnectFromLao,
  laoReduce, makeCurrentLao, makeIsLaoOrganizer, makeLao, makeLaoIdsList, makeLaosList, makeLaosMap,
  removeLao, setLaoLastRollCall,
  updateLao,
} from '../LaoReducer';
import {
  mockLaoCreationTime,
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  org,
} from './SocialReducer.test';
import { rollCallId } from './EventsReducer.test';

const emptyState = {
  byId: {},
  allIds: [],
};

const lao = new Lao({
  id: mockLaoIdHash,
  name: mockLaoName,
  creation: mockLaoCreationTime,
  last_modified: mockLaoCreationTime,
  organizer: org,
  witnesses: [],
}).toState();

const filledState1 = {
  byId: { [mockLaoId]: lao },
  allIds: [mockLaoId],
};

const laoAfterRollCall = new Lao({
  id: mockLaoIdHash,
  name: mockLaoName,
  creation: mockLaoCreationTime,
  last_modified: mockLaoCreationTime,
  organizer: org,
  witnesses: [],
  last_roll_call_id: rollCallId,
  last_tokenized_roll_call_id: rollCallId,
}).toState();

const filledStateAfterRollCall = {
  byId: { [mockLaoId]: laoAfterRollCall },
  allIds: [mockLaoId],
};

const mockLao2Name = 'Second Lao';
const mockLao2IdHash: Hash = Hash.fromStringArray(
  org.toString(), mockLaoCreationTime.toString(), mockLao2Name,
);
const mockLao2Id = mockLao2IdHash.toString();

const lao2 = new Lao({
  id: mockLao2IdHash,
  name: mockLao2Name,
  creation: mockLaoCreationTime,
  last_modified: mockLaoCreationTime,
  organizer: org,
  witnesses: [],
}).toState();

const laoUpdated = new Lao({
  id: mockLaoIdHash,
  name: mockLaoName,
  creation: mockLaoCreationTime,
  last_modified: new Timestamp(1606666600),
  organizer: org,
  witnesses: [],
}).toState();

const filledStateUpdated = {
  byId: { [mockLaoId]: laoUpdated },
  allIds: [mockLaoId],
};

const filledState2 = {
  byId: {
    [mockLaoId]: lao,
    [mockLao2Id]: lao2,
  },
  allIds: [mockLaoId, mockLao2Id],
};

const connectedState1 = {
  byId: { [mockLaoId]: lao },
  allIds: [mockLaoId],
  currentId: mockLaoId,
};

const laoRecord = { [mockLaoId]: lao };

describe('LaoReducer', () => {
  it('should return the initial state', () => {
    expect(laoReduce(undefined, {} as AnyAction))
      .toEqual(emptyState);
  });

  it('should add lao', () => {
    expect(laoReduce(emptyState, addLao(lao)))
      .toEqual(filledState1);
  });

  it('should not add a lao twice', () => {
    expect(laoReduce(filledState1, addLao(lao)))
      .toEqual(filledState1);
  });

  it('should not update lao if it is not in store', () => {
    expect(laoReduce(filledState1, updateLao(lao2)))
      .toEqual(filledState1);
  });

  it('should update lao if its id is in store', () => {
    expect(laoReduce(filledState1, updateLao(laoUpdated)))
      .toEqual(filledStateUpdated);
  });

  it('should remove a lao', () => {
    expect(laoReduce(filledState1, removeLao(mockLaoIdHash)))
      .toEqual(emptyState);
  });

  it('should not remove a non-stored lao', () => {
    expect(laoReduce(filledState1, removeLao(mockLao2IdHash)))
      .toEqual(filledState1);
  });

  it('should clear all Laos', () => {
    expect(laoReduce(filledState2, clearAllLaos()))
      .toEqual(emptyState);
  });

  it('should connect to lao', () => {
    expect(laoReduce(emptyState, connectToLao(lao)))
      .toEqual(connectedState1);
  });

  it('should disconnect from lao', () => {
    expect(laoReduce(connectedState1, disconnectFromLao()))
      .toEqual(filledState1);
  });

  it('set last roll call for a non-stored lao does not do anything', () => {
    expect(laoReduce(emptyState, setLaoLastRollCall(mockLaoId, rollCallId, false)))
      .toEqual(emptyState);
  });

  it('set last roll call for lao', () => {
    expect(laoReduce(filledState1, setLaoLastRollCall(mockLaoId, rollCallId, true)))
      .toEqual(filledStateAfterRollCall);
  });
});

describe('Lao selector', () => {
  it('should return undefined for makeLao when the currentId is undefined', () => {
    expect(makeLao().resultFunc(laoRecord, undefined))
      .toEqual(undefined);
  });

  it('should return lao for makeCurrentLao', () => {
    expect(makeCurrentLao().resultFunc(laoRecord, mockLaoId))
      .toEqual(Lao.fromState(lao));
  });

  it('should return an empty makeLaoIdsList', () => {
    expect(makeLaoIdsList().resultFunc([]))
      .toEqual([]);
  });

  it('should return makeLaosList correctly', () => {
    expect(makeLaosList().resultFunc(laoRecord, [mockLaoId]))
      .toEqual([Lao.fromState(lao)]);
  });

  it('should return makeLaosMap correctly', () => {
    expect(makeLaosMap().resultFunc(laoRecord))
      .toEqual({ [mockLaoId]: Lao.fromState(lao) });
  });

  it('should return true for makeIsLaoOrganizer', () => {
    expect(makeIsLaoOrganizer().resultFunc(laoRecord, mockLaoId, org.toString()))
      .toEqual(true);
  });
});
