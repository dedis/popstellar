import { configureStore } from '@reduxjs/toolkit';
import { AnyAction, combineReducers } from 'redux';

import {
  mockAddress,
  mockKeyPair,
  serializedMockLaoId,
  mockPublicKey,
  mockPublicKey2,
  org,
  mockLaoId,
  mockPopToken,
  mockLao,
} from '__tests__/utils';
import { Hash } from 'core/objects';
import { LaoServer } from 'features/lao/objects/LaoServer';

import { laoReducer, serverReducer } from '../index';
import { setCurrentLao } from '../LaoReducer';
import {
  addServer,
  clearAllServers,
  makeLaoOrganizerBackendPublicKeySelector,
  removeServer,
  serverReduce,
  ServerReducerState,
  updateServer,
} from '../LaoServerReducer';

const emptyState = {
  byLaoId: {},
} as ServerReducerState;

const otherAddress = 'some other address';
const mockServer = new LaoServer({
  laoId: mockLaoId,
  address: mockAddress,
  serverPublicKey: mockKeyPair.publicKey,
  frontendPublicKey: mockPopToken.publicKey,
});
const mockServerState = mockServer.toState();

const otherMockServer = new LaoServer({
  laoId: mockLaoId,
  address: otherAddress,
  serverPublicKey: mockPopToken.publicKey,
  frontendPublicKey: mockKeyPair.publicKey,
});
const otherMockServerState = otherMockServer.toState();

describe('LaoServerReducer', () => {
  it('should return the initial state', () => {
    expect(serverReduce(undefined, {} as AnyAction)).toEqual(emptyState);
  });

  describe('addServer', () => {
    it('should add a server to the store', () => {
      expect(
        serverReduce(
          {
            byLaoId: {
              [serializedMockLaoId]: {
                allAddresses: [otherAddress],
                byAddress: { [otherAddress]: otherMockServerState },
                backendKeyByFrontendKey: {
                  [mockPublicKey]: mockPublicKey2,
                },
              },
            },
          } as ServerReducerState,
          addServer(mockServer),
        ),
      ).toEqual({
        byLaoId: {
          [serializedMockLaoId]: {
            allAddresses: [otherAddress, mockAddress],
            byAddress: {
              [mockAddress]: mockServerState,
              [otherAddress]: otherMockServerState,
            },
            backendKeyByFrontendKey: {
              [mockPublicKey]: mockPublicKey2,
              [mockPublicKey2]: mockPublicKey,
            },
          },
        },
      } as ServerReducerState);
    });

    it('should throw if the same address is added twice', () => {
      const fn = () =>
        serverReduce(
          {
            byLaoId: {
              [serializedMockLaoId]: {
                allAddresses: [mockAddress],
                byAddress: { [mockAddress]: mockServerState },
                backendKeyByFrontendKey: {
                  [mockPublicKey2]: mockPublicKey,
                },
              },
            },
          } as ServerReducerState,
          addServer(mockServer),
        );

      expect(fn).toThrow(Error);
    });
  });

  describe('updateServer', () => {
    it('should update the server state for address of the given server', () => {
      expect(
        serverReduce(
          {
            byLaoId: {
              [serializedMockLaoId]: {
                allAddresses: [otherAddress, mockAddress],
                byAddress: {
                  [mockAddress]: mockServerState,
                  [otherAddress]: otherMockServerState,
                },
                backendKeyByFrontendKey: {
                  [mockPublicKey]: mockPublicKey2,
                  [mockPublicKey2]: mockPublicKey,
                },
              },
            },
          } as ServerReducerState,
          updateServer(mockServer.laoId, mockServer.address, {
            ...mockServerState,
            serverPublicKey: mockPublicKey2,
          }),
        ),
      ).toEqual({
        byLaoId: {
          [serializedMockLaoId]: {
            allAddresses: [otherAddress, mockAddress],
            byAddress: {
              [mockAddress]: {
                ...mockServerState,
                serverPublicKey: mockPublicKey2,
              },
              [otherAddress]: otherMockServerState,
            },
            backendKeyByFrontendKey: {
              [mockPublicKey]: mockPublicKey2,
              [mockPublicKey2]: mockPublicKey2,
            },
          },
        },
      } as ServerReducerState);
    });
  });

  describe('removeServer', () => {
    it('should remove exactly the server with the provided address', () => {
      expect(
        serverReduce(
          {
            byLaoId: {
              [serializedMockLaoId]: {
                allAddresses: [mockAddress, otherAddress],
                byAddress: {
                  [mockAddress]: mockServerState,
                  [otherAddress]: otherMockServerState,
                },
                backendKeyByFrontendKey: {
                  [mockPublicKey]: mockPublicKey2,
                  [mockPublicKey2]: mockPublicKey,
                },
              },
            },
          } as ServerReducerState,
          removeServer(mockLaoId, otherAddress),
        ),
      ).toEqual({
        byLaoId: {
          [serializedMockLaoId]: {
            allAddresses: [mockAddress],
            byAddress: { [mockAddress]: mockServerState },
            backendKeyByFrontendKey: {
              [mockPublicKey2]: mockPublicKey,
            },
          },
        },
      } as ServerReducerState);
    });
  });

  describe('clearAllServers', () => {
    it('should return an empty state', () => {
      expect(
        serverReduce(
          {
            byLaoId: {
              [serializedMockLaoId]: {
                allAddresses: [mockAddress, otherAddress],
                byAddress: {
                  [mockAddress]: mockServerState,
                  [otherAddress]: otherMockServerState,
                },
                backendKeyByFrontendKey: {
                  [mockPublicKey]: mockPublicKey2,
                  [mockPublicKey2]: mockPublicKey,
                },
              },
            },
          } as ServerReducerState,
          clearAllServers(),
        ),
      ).toEqual(emptyState);
    });
  });

  describe('makeLaoOrganizerBackendPublicKeySelector', () => {
    it('should return the correct value if a lao and server entry exist', () => {
      const mockStore = configureStore({
        reducer: combineReducers({
          ...laoReducer,
          ...serverReducer,
        }),
      });
      mockStore.dispatch(setCurrentLao(mockLao));

      mockStore.dispatch(
        addServer(
          new LaoServer({
            address: mockAddress,
            laoId: mockLaoId,
            frontendPublicKey: org,
            serverPublicKey: mockKeyPair.publicKey,
          }),
        ),
      );

      expect(
        makeLaoOrganizerBackendPublicKeySelector(mockLaoId)(mockStore.getState())?.valueOf(),
      ).toEqual(org.valueOf());
    });

    it('should return undefined if there is no lao entry for the given id', () => {
      const mockStore = configureStore({
        reducer: combineReducers({
          ...laoReducer,
          ...serverReducer,
        }),
      });
      mockStore.dispatch(
        addServer(
          new LaoServer({
            address: mockAddress,
            laoId: mockLaoId,
            frontendPublicKey: org,
            serverPublicKey: mockKeyPair.publicKey,
          }),
        ),
      );

      expect(
        makeLaoOrganizerBackendPublicKeySelector(mockLaoId)(mockStore.getState()),
      ).toBeUndefined();
    });

    it('should return undefined if there is no server entry for the organizer public key', () => {
      const mockStore = configureStore({
        reducer: combineReducers({
          ...laoReducer,
          ...serverReducer,
        }),
      });
      mockStore.dispatch(setCurrentLao(mockLao));
      // add the organizers public key but for a *different* lao
      mockStore.dispatch(
        addServer(
          new LaoServer({
            address: mockAddress,
            laoId: new Hash('someOtherLao'),
            frontendPublicKey: org,
            serverPublicKey: mockKeyPair.publicKey,
          }),
        ),
      );

      expect(
        makeLaoOrganizerBackendPublicKeySelector(mockLaoId)(mockStore.getState()),
      ).toBeUndefined();
    });
  });
});
