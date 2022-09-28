import { configureStore } from '@reduxjs/toolkit';
import { AnyAction, combineReducers } from 'redux';

import {
  mockAddress,
  mockKeyPair,
  mockLaoId,
  mockLaoState,
  mockPublicKey,
  mockPublicKey2,
  org,
} from '__tests__/utils';
import { ServerState } from 'features/lao/objects/LaoServer';

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
} from '../ServerReducer';

const emptyState = {
  byLaoId: {},
} as ServerReducerState;

const otherAddress = 'some other address';
const mockServerState: ServerState = {
  laoId: mockLaoId,
  address: mockAddress,
  serverPublicKey: mockPublicKey,
  frontendPublicKey: mockPublicKey2,
};
const otherMockServerState: ServerState = {
  laoId: mockLaoId,
  address: otherAddress,
  serverPublicKey: mockPublicKey2,
  frontendPublicKey: mockPublicKey,
};

describe('ServerReducer', () => {
  it('should return the initial state', () => {
    expect(serverReduce(undefined, {} as AnyAction)).toEqual(emptyState);
  });

  describe('addServer', () => {
    it('should add a server to the store', () => {
      expect(
        serverReduce(
          {
            byLaoId: {
              [mockLaoId]: {
                allAddresses: [otherAddress],
                byAddress: { [otherAddress]: otherMockServerState },
                backendKeyByFrontendKey: {
                  [mockPublicKey]: mockPublicKey2,
                },
              },
            },
          } as ServerReducerState,
          addServer(mockServerState),
        ),
      ).toEqual({
        byLaoId: {
          [mockLaoId]: {
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
              [mockLaoId]: {
                allAddresses: [mockAddress],
                byAddress: { [mockAddress]: mockServerState },
                backendKeyByFrontendKey: {
                  [mockPublicKey2]: mockPublicKey,
                },
              },
            },
          } as ServerReducerState,
          addServer(mockServerState),
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
              [mockLaoId]: {
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
          updateServer({
            ...mockServerState,
            serverPublicKey: mockPublicKey2,
          }),
        ),
      ).toEqual({
        byLaoId: {
          [mockLaoId]: {
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
              [mockLaoId]: {
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
          removeServer({
            laoId: mockLaoId,
            address: otherAddress,
          }),
        ),
      ).toEqual({
        byLaoId: {
          [mockLaoId]: {
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
              [mockLaoId]: {
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
      mockStore.dispatch(setCurrentLao(mockLaoState));

      mockStore.dispatch(
        addServer({
          address: mockAddress,
          laoId: mockLaoId,
          frontendPublicKey: org.valueOf(),
          serverPublicKey: mockKeyPair.publicKey.valueOf(),
        }),
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
        addServer({
          address: mockAddress,
          laoId: mockLaoId,
          frontendPublicKey: org.valueOf(),
          serverPublicKey: mockKeyPair.publicKey.valueOf(),
        }),
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
      mockStore.dispatch(setCurrentLao(mockLaoState));
      // add the organizers public key but for a *different* lao
      mockStore.dispatch(
        addServer({
          address: mockAddress,
          laoId: 'someOtherLao',
          frontendPublicKey: org.valueOf(),
          serverPublicKey: mockKeyPair.publicKey.valueOf(),
        }),
      );

      expect(
        makeLaoOrganizerBackendPublicKeySelector(mockLaoId)(mockStore.getState()),
      ).toBeUndefined();
    });
  });
});
