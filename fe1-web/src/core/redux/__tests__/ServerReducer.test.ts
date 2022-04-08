import { AnyAction } from 'redux';

import { mockAddress, mockPublicKey, mockPublicKey2 } from '__tests__/utils';
import { ServerState } from 'core/objects';

import {
  addServer,
  clearAllServers,
  removeServer,
  serverReduce,
  ServerReducerState,
  updateServer,
} from '../ServerReducer';

const emptyState = {
  allAddresses: [],
  byAddress: {},
} as ServerReducerState;

const otherAddress = 'some other address';
const mockServerState: ServerState = {
  address: mockAddress,
  publicKey: mockPublicKey,
};
const otherMockServerState: ServerState = {
  address: otherAddress,
  publicKey: mockPublicKey2,
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
            allAddresses: [otherAddress],
            byAddress: { [otherAddress]: otherMockServerState },
          } as ServerReducerState,
          addServer(mockServerState),
        ),
      ).toEqual({
        allAddresses: [otherAddress, mockAddress],
        byAddress: { [mockAddress]: mockServerState, [otherAddress]: otherMockServerState },
      } as ServerReducerState);
    });
  });

  describe('updateServer', () => {
    it('should update the server state for address of the given server', () => {
      expect(
        serverReduce(
          {
            allAddresses: [otherAddress, mockAddress],
            byAddress: { [mockAddress]: mockServerState, [otherAddress]: otherMockServerState },
          } as ServerReducerState,
          updateServer({ ...mockServerState, publicKey: mockPublicKey2 }),
        ),
      ).toEqual({
        allAddresses: [otherAddress, mockAddress],
        byAddress: {
          [mockAddress]: { ...mockServerState, publicKey: mockPublicKey2 },
          [otherAddress]: otherMockServerState,
        },
      } as ServerReducerState);
    });
  });

  describe('removeServer', () => {
    it('should remove exactly the server with the provided address', () => {
      expect(
        serverReduce(
          {
            allAddresses: [mockAddress, otherAddress],
            byAddress: { [mockAddress]: mockServerState, [otherAddress]: otherMockServerState },
          } as ServerReducerState,
          removeServer(otherAddress),
        ),
      ).toEqual({
        allAddresses: [mockAddress],
        byAddress: { [mockAddress]: mockServerState },
      } as ServerReducerState);
    });
  });

  describe('clearAllServers', () => {
    it('should return an empty state', () => {
      expect(
        serverReduce(
          {
            allAddresses: [mockAddress, otherAddress],
            byAddress: { [mockAddress]: mockServerState, [otherAddress]: otherMockServerState },
          } as ServerReducerState,
          clearAllServers(),
        ),
      ).toEqual(emptyState);
    });
  });
});
