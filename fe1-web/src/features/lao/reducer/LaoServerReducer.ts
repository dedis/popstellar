/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { Hash, PublicKey } from 'core/objects';

import { LaoServer, ServerAddress, LaoServerState } from '../objects/LaoServer';
import { getLaosState, LaoReducerState } from './LaoReducer';

/**
 * Reducer & associated function implementation to store all known servers
 */

export interface ServerReducerState {
  byLaoId: {
    [laoId: string]: {
      byAddress: {
        [address: string]: LaoServerState;
      };
      allAddresses: ServerAddress[];
      backendKeyByFrontendKey: {
        [frontendKey: string]: string;
      };
    };
  };
}

const initialState: ServerReducerState = {
  byLaoId: {},
};

export const SERVER_REDUCER_PATH = 'servers';

const serverSlice = createSlice({
  name: SERVER_REDUCER_PATH,
  initialState,
  reducers: {
    addServer: {
      prepare: (server: LaoServer) => ({
        payload: server.toState(),
      }),
      reducer: (state: Draft<ServerReducerState>, action: PayloadAction<LaoServerState>) => {
        const server = action.payload;

        let laoState = {
          byAddress: {} as {
            [address: string]: LaoServerState;
          },
          allAddresses: [] as string[],
          backendKeyByFrontendKey: {} as {
            [frontendKey: string]: string;
          },
        };

        if (server.laoId in state.byLaoId) {
          laoState = state.byLaoId[server.laoId];
        }

        if (server.address in laoState.byAddress) {
          throw Error(
            `${server.address} is already part of laoState.byAddress. Use updateServer() instead of addServer()`,
          );
        }

        laoState.byAddress[server.address] = server;
        laoState.allAddresses.push(server.address);

        laoState.backendKeyByFrontendKey[server.frontendPublicKey] = server.serverPublicKey;
        state.byLaoId[server.laoId] = laoState;
      },
    },

    updateServer: {
      prepare: (
        laoId: Hash,
        address: ServerAddress,
        serverStateUpdate: Partial<LaoServerState>,
      ) => ({
        payload: {
          laoId: laoId.serialize(),
          address,
          serverStateUpdate,
        },
      }),
      reducer: (
        state: Draft<ServerReducerState>,
        action: PayloadAction<{
          laoId: string;
          address: string;
          serverStateUpdate: Partial<LaoServerState>;
        }>,
      ) => {
        const { laoId, address, serverStateUpdate } = action.payload;

        if (!(laoId in state.byLaoId)) {
          return;
        }

        if (!(address in state.byLaoId[laoId].byAddress)) {
          return;
        }

        const updatedServer: LaoServerState = {
          ...state.byLaoId[laoId].byAddress[address],
          ...serverStateUpdate,
        };

        // delete old frontend-backend key mapping entry
        delete state.byLaoId[laoId].backendKeyByFrontendKey[
          state.byLaoId[laoId].byAddress[address].frontendPublicKey
        ];

        // update byAddress entry
        state.byLaoId[laoId].byAddress[updatedServer.address] = updatedServer;

        // add new frontend-backend key mapping entry
        state.byLaoId[laoId].backendKeyByFrontendKey[updatedServer.frontendPublicKey] =
          updatedServer.serverPublicKey;
      },
    },

    removeServer: {
      prepare: (laoId: Hash, address: ServerAddress) => ({
        payload: { laoId: laoId.serialize(), address },
      }),
      reducer: (state, action: PayloadAction<{ laoId: string; address: string }>) => {
        const { laoId, address: serverAddress } = action.payload;

        if (!(laoId in state.byLaoId)) {
          return;
        }

        if (serverAddress in state.byLaoId[laoId].byAddress) {
          // delete the frontend-backend key mapping entry
          delete state.byLaoId[laoId].backendKeyByFrontendKey[
            state.byLaoId[laoId].byAddress[serverAddress].frontendPublicKey
          ];

          // cleanup the byAddress and allAddress entries
          delete state.byLaoId[laoId].byAddress[serverAddress];
          state.byLaoId[laoId].allAddresses = state.byLaoId[laoId].allAddresses.filter(
            (address) => address !== serverAddress,
          );
        }
      },
    },

    clearAllServers: (state) => {
      state.byLaoId = {};
    },
  },
});

export const { addServer, clearAllServers, removeServer, updateServer } = serverSlice.actions;

export const serverReduce = serverSlice.reducer;

export default {
  [SERVER_REDUCER_PATH]: serverSlice.reducer,
};

export const getServerState = (state: any): ServerReducerState => state[SERVER_REDUCER_PATH];

/**
 * A function to directly retrieve the public key from the redux store for a given lao id and server address
 * @remark This function does not memoize the result. If you need this, use makeServerSelector instead
 * @param laoId The lao id
 * @param address The server address
 * @param state The redux state
 * @returns The public key for the given server address or undefined if there is none
 */
export const getServerPublicKeyByAddress = (
  laoId: Hash,
  address: ServerAddress,
  state: any,
): PublicKey | undefined => {
  const serverState = getServerState(state);
  const serializedLaoId = laoId.serialize();

  if (
    serializedLaoId in serverState.byLaoId &&
    address in serverState.byLaoId[serializedLaoId].byAddress
  ) {
    return new PublicKey(serverState.byLaoId[serializedLaoId].byAddress[address].serverPublicKey);
  }

  return undefined;
};

/**
 * A function that creates a selector that retrieve the public key of the lao organizer's backend
 * @param laoId The lao id
 * @returns The public key of the lao organizer's backend and undefined if there is none
 */
export const makeLaoOrganizerBackendPublicKeySelector = (laoId?: Hash) =>
  createSelector(
    // First input: The server state
    (state: any) => getServerState(state),
    // Second input: The laos state
    (state: any) => getLaosState(state),
    // Selector: returns the server object associated to the given address
    (serverState: ServerReducerState, laoState: LaoReducerState): PublicKey | undefined => {
      const serializedLaoId = laoId?.serialize();

      // if there is no current lao, return undefined
      if (
        !serializedLaoId ||
        !(serializedLaoId in laoState.byId) ||
        !(serializedLaoId in serverState.byLaoId)
      ) {
        return undefined;
      }

      const currentLaoState = laoState.byId[serializedLaoId];

      if (
        currentLaoState.organizer in serverState.byLaoId[serializedLaoId].backendKeyByFrontendKey
      ) {
        return new PublicKey(
          serverState.byLaoId[serializedLaoId].backendKeyByFrontendKey[currentLaoState.organizer],
        );
      }

      return undefined;
    },
  );

/**
 * Creates a server selector for a given lao id and server address. Can for example be used in useSelector()
 * @param laoId The lao id
 * @param address The server address
 * @returns A selector for the server object for the given address or undefined if there is none
 */
export const makeServerSelector = (laoId: Hash, address: ServerAddress) => {
  const serializedLaoId = laoId.serialize();

  return createSelector(
    // First input: map of lao ids to servers
    (state: any) => getServerState(state).byLaoId,
    // Selector: returns the server object associated to the given address
    (byLaoId: ServerReducerState['byLaoId']): LaoServer | undefined => {
      if (serializedLaoId in byLaoId && address in byLaoId[serializedLaoId].byAddress) {
        return LaoServer.fromState(byLaoId[serializedLaoId].byAddress[address]);
      }

      return undefined;
    },
  );
};
