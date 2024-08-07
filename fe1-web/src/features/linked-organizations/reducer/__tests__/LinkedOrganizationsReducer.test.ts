import { describe } from '@jest/globals';
import { AnyAction } from 'redux';

import { mockLaoId, mockLaoId2, mockPublicKey, serializedMockLaoId } from '__tests__/utils';
import { Hash, PublicKey, Timestamp } from 'core/objects';
import { Challenge } from 'features/linked-organizations/objects/Challenge';
import { LinkedOrganizationState } from 'features/linked-organizations/objects/LinkedOrganization';

import {
  addLinkedOrganization,
  addScannedLinkedOrganization,
  LinkedOrganizationReducerState,
  LINKEDORGANIZATIONS_REDUCER_PATH,
  linkedOrganizationsReduce,
  makeLinkedOrganizationSelector,
  makeScannedLinkedOrganizationSelector,
  makeSingleLinkedOrganizationSelector,
  removeScannedLinkedOrganization,
} from '../LinkedOrganizationsReducer';

const mockChallenge: Challenge = new Challenge({
  value: new Hash('82520f235f413b26571529f69d53d751335873efca97e15cd7c47d063ead830d'),
  valid_until: Timestamp.EpochNow().addSeconds(86400),
});

const mockOrganizationState = {
  lao_id: mockLaoId.toState(),
  server_address: 'wss://epfl.ch:9000/server',
  public_key: new PublicKey(mockPublicKey).toState(),
  challenge: mockChallenge.toState(),
} as LinkedOrganizationState;

describe('LinkedOrganizationReducer', () => {
  describe('returns a valid initial state', () => {
    it('returns a valid initial state', () => {
      expect(linkedOrganizationsReduce(undefined, {} as AnyAction)).toEqual({
        byLaoId: {},
      } as LinkedOrganizationReducerState);
    });
  });

  describe('addLinkedOrganization', () => {
    it('adds new linked organization to the state', () => {
      const serializedMockLaoId2 = mockLaoId2.valueOf();
      const newState = linkedOrganizationsReduce(
        {
          byLaoId: {},
        } as LinkedOrganizationReducerState,
        addLinkedOrganization(mockLaoId2, mockOrganizationState),
      );
      expect(newState.byLaoId[serializedMockLaoId2].allLaoIds).toEqual([serializedMockLaoId]);
      expect(newState.byLaoId[serializedMockLaoId2].byLinkedLaoId).toHaveProperty(
        serializedMockLaoId,
        mockOrganizationState,
      );
    });

    it('throws an error if the store already contains an linked organization with the same id', () => {
      const serializedMockLaoId2 = mockLaoId2.valueOf();
      const newState = linkedOrganizationsReduce(
        {
          byLaoId: {},
        } as LinkedOrganizationReducerState,
        addLinkedOrganization(mockLaoId2, mockOrganizationState),
      );
      expect(newState.byLaoId[serializedMockLaoId2].allLaoIds).toEqual([serializedMockLaoId]);
      expect(newState.byLaoId[serializedMockLaoId2].byLinkedLaoId).toHaveProperty(
        serializedMockLaoId,
        mockOrganizationState,
      );
      expect(() =>
        linkedOrganizationsReduce(
          newState,
          addLinkedOrganization(mockLaoId2, mockOrganizationState),
        ),
      ).toThrow();
    });
  });

  describe('addScannedLinkedOrganization', () => {
    it('adds new scanned linked organization to the state', () => {
      const serializedMockLaoId2 = mockLaoId2.valueOf();
      const newState = linkedOrganizationsReduce(
        {
          byLaoId: {},
        } as LinkedOrganizationReducerState,
        addScannedLinkedOrganization(mockLaoId2, mockOrganizationState),
      );
      expect(newState.byLaoId[serializedMockLaoId2].allScannedLaos).toEqual([
        mockOrganizationState,
      ]);
    });

    it('throws an error if the store already contains an linked organization with the same id', () => {
      const serializedMockLaoId2 = mockLaoId2.valueOf();
      const newState = linkedOrganizationsReduce(
        {
          byLaoId: {},
        } as LinkedOrganizationReducerState,
        addLinkedOrganization(mockLaoId2, mockOrganizationState),
      );
      expect(newState.byLaoId[serializedMockLaoId2].allLaoIds).toEqual([serializedMockLaoId]);
      expect(() =>
        linkedOrganizationsReduce(
          newState,
          addScannedLinkedOrganization(mockLaoId2, mockOrganizationState),
        ),
      ).toThrow();
    });
  });

  describe('removeScannedLinkedOrganization', () => {
    it('removes new scanned linked organization from the state', () => {
      const serializedMockLaoId2 = mockLaoId2.valueOf();
      let newState = linkedOrganizationsReduce(
        {
          byLaoId: {},
        } as LinkedOrganizationReducerState,
        addScannedLinkedOrganization(mockLaoId2, mockOrganizationState),
      );
      newState = linkedOrganizationsReduce(
        newState,
        removeScannedLinkedOrganization(mockLaoId2, mockOrganizationState.lao_id),
      );
      expect(newState.byLaoId[serializedMockLaoId2].allScannedLaos).toEqual([]);
    });
  });
});

describe('makeSingleLinkedOrganizationsSelector', () => {
  it('returns the correct linked organization', () => {
    const serializedMockLaoId2 = mockLaoId2.valueOf();
    const newState = linkedOrganizationsReduce(
      {
        byLaoId: {},
      } as LinkedOrganizationReducerState,
      addLinkedOrganization(mockLaoId2, mockOrganizationState),
    );
    expect(newState.byLaoId[serializedMockLaoId2].allLaoIds).toEqual([serializedMockLaoId]);
    expect(newState.byLaoId[serializedMockLaoId2].byLinkedLaoId).toHaveProperty(
      serializedMockLaoId,
      mockOrganizationState,
    );
    expect(
      makeSingleLinkedOrganizationSelector(
        mockLaoId2,
        serializedMockLaoId,
      )({
        [LINKEDORGANIZATIONS_REDUCER_PATH]: {
          byLaoId: {
            [serializedMockLaoId2]: {
              allLaoIds: [serializedMockLaoId],
              byLinkedLaoId: { [serializedMockLaoId]: mockOrganizationState },
            },
          },
        } as LinkedOrganizationReducerState,
      }),
    ).toEqual(mockOrganizationState);
  });

  it('returns undefined if the linked organization is not in the store', () => {
    const serializedMockLaoId2 = mockLaoId2.valueOf();
    const newState = linkedOrganizationsReduce(
      {
        byLaoId: {},
      } as LinkedOrganizationReducerState,
      addLinkedOrganization(mockLaoId2, mockOrganizationState),
    );
    expect(newState.byLaoId[serializedMockLaoId2].allLaoIds).toEqual([serializedMockLaoId]);
    expect(newState.byLaoId[serializedMockLaoId2].byLinkedLaoId).toHaveProperty(
      serializedMockLaoId,
      mockOrganizationState,
    );
    expect(
      makeSingleLinkedOrganizationSelector(
        mockLaoId2,
        'false-lao-id',
      )({
        [LINKEDORGANIZATIONS_REDUCER_PATH]: {
          byLaoId: {
            [serializedMockLaoId2]: {
              allLaoIds: [serializedMockLaoId],
              byLinkedLaoId: { [serializedMockLaoId]: mockOrganizationState },
            },
          },
        } as LinkedOrganizationReducerState,
      }),
    ).toBeUndefined();
  });
});

describe('makeLinkedOrganizationsSelector', () => {
  it('returns the correct linked organization', () => {
    const serializedMockLaoId2 = mockLaoId2.valueOf();
    const newState = linkedOrganizationsReduce(
      {
        byLaoId: {},
      } as LinkedOrganizationReducerState,
      addLinkedOrganization(mockLaoId2, mockOrganizationState),
    );
    expect(newState.byLaoId[serializedMockLaoId2].allLaoIds).toEqual([serializedMockLaoId]);
    expect(newState.byLaoId[serializedMockLaoId2].byLinkedLaoId).toHaveProperty(
      serializedMockLaoId,
      mockOrganizationState,
    );
    expect(
      makeLinkedOrganizationSelector(mockLaoId2)({
        [LINKEDORGANIZATIONS_REDUCER_PATH]: {
          byLaoId: {
            [serializedMockLaoId2]: {
              allLaoIds: [serializedMockLaoId],
              byLinkedLaoId: { [serializedMockLaoId]: mockOrganizationState },
              allLaos: [mockOrganizationState],
            },
          },
        } as LinkedOrganizationReducerState,
      }),
    ).toEqual([serializedMockLaoId]);
  });

  it('returns empty array if the linked organization  is not in the store', () => {
    const serializedMockLaoId2 = mockLaoId2.valueOf();
    const newState = linkedOrganizationsReduce(
      {
        byLaoId: {},
      } as LinkedOrganizationReducerState,
      addLinkedOrganization(mockLaoId2, mockOrganizationState),
    );
    expect(newState.byLaoId[serializedMockLaoId2].allLaoIds).toEqual([serializedMockLaoId]);
    expect(newState.byLaoId[serializedMockLaoId2].byLinkedLaoId).toHaveProperty(
      serializedMockLaoId,
      mockOrganizationState,
    );
    expect(
      makeLinkedOrganizationSelector(new Hash('false-lao-id'))({
        [LINKEDORGANIZATIONS_REDUCER_PATH]: {
          byLaoId: {
            [serializedMockLaoId2]: {
              allLaoIds: [serializedMockLaoId],
              byLinkedLaoId: { [serializedMockLaoId]: mockOrganizationState },
            },
          },
        } as LinkedOrganizationReducerState,
      }),
    ).toEqual([]);
  });

  describe('makeScannedLinkedOrganizationSelector', () => {
    it('returns the correct scanned linked organization', () => {
      const serializedMockLaoId2 = mockLaoId2.valueOf();
      const newState = linkedOrganizationsReduce(
        {
          byLaoId: {},
        } as LinkedOrganizationReducerState,
        addScannedLinkedOrganization(mockLaoId2, mockOrganizationState),
      );
      expect(newState.byLaoId[serializedMockLaoId2].allScannedLaos).toEqual([
        mockOrganizationState,
      ]);
      expect(
        makeScannedLinkedOrganizationSelector(mockLaoId2)({
          [LINKEDORGANIZATIONS_REDUCER_PATH]: {
            byLaoId: {
              [serializedMockLaoId2]: {
                allLaoIds: [],
                byLinkedLaoId: {},
                allLaos: [],
                allScannedLaos: [mockOrganizationState],
              },
            },
          } as LinkedOrganizationReducerState,
        }),
      ).toEqual([mockOrganizationState]);
    });

    it('returns empty array if the scanned linked organization  is not in the store', () => {
      const serializedMockLaoId2 = mockLaoId2.valueOf();
      const newState = linkedOrganizationsReduce(
        {
          byLaoId: {},
        } as LinkedOrganizationReducerState,
        addScannedLinkedOrganization(mockLaoId2, mockOrganizationState),
      );
      expect(newState.byLaoId[serializedMockLaoId2].allScannedLaos).toEqual([
        mockOrganizationState,
      ]);
      expect(
        makeScannedLinkedOrganizationSelector(mockLaoId)({
          [LINKEDORGANIZATIONS_REDUCER_PATH]: {
            byLaoId: {
              [serializedMockLaoId2]: {
                allLaoIds: [],
                byLinkedLaoId: {},
                allLaos: [],
                allScannedLaos: [mockOrganizationState],
              },
            },
          } as LinkedOrganizationReducerState,
        }),
      ).toEqual([]);
    });
  });
});
