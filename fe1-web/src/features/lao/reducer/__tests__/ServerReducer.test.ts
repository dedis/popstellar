import { AnyAction } from 'redux';

import { mockAddress, mockLaoId, mockPublicKey, mockPublicKey2 } from '__tests__/utils';
import { ServerState } from 'features/lao/objects/Server';

import {
  addServer,
  clearAllServers,
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
  publicKey: mockPublicKey,
};
const otherMockServerState: ServerState = {
  laoId: mockLaoId,
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
            byLaoId: {
              [mockLaoId]: {
                allAddresses: [otherAddress],
                byAddress: { [otherAddress]: otherMockServerState },
              },
            },
          } as ServerReducerState,
          addServer(mockServerState),
        ),
      ).toEqual({
        byLaoId: {
          [mockLaoId]: {
            allAddresses: [otherAddress, mockAddress],
            byAddress: { [mockAddress]: mockServerState, [otherAddress]: otherMockServerState },
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
                byAddress: { [mockAddress]: mockServerState, [otherAddress]: otherMockServerState },
              },
            },
          } as ServerReducerState,
          updateServer({ ...mockServerState, publicKey: mockPublicKey2 }),
        ),
      ).toEqual({
        byLaoId: {
          [mockLaoId]: {
            allAddresses: [otherAddress, mockAddress],
            byAddress: {
              [mockAddress]: { ...mockServerState, publicKey: mockPublicKey2 },
              [otherAddress]: otherMockServerState,
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
                byAddress: { [mockAddress]: mockServerState, [otherAddress]: otherMockServerState },
              },
            },
          } as ServerReducerState,
          removeServer({ laoId: mockLaoId, address: otherAddress }),
        ),
      ).toEqual({
        byLaoId: {
          [mockLaoId]: {
            allAddresses: [mockAddress],
            byAddress: { [mockAddress]: mockServerState },
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
                byAddress: { [mockAddress]: mockServerState, [otherAddress]: otherMockServerState },
              },
            },
          } as ServerReducerState,
          clearAllServers(),
        ),
      ).toEqual(emptyState);
    });
  });
});
