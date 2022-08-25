/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { PublicKey } from 'core/objects';

import { LaoServer, ServerAddress, ServerState } from '../objects/LaoServer';
import { getLaosState, LaoReducerState } from './LaoReducer';

/**
 * Reducer & associated function implementation to store all known servers
 */

export interface ServerReducerState {
  byLaoId: {
    [laoId: string]: {
      byAddress: {
        [address: string]: ServerState;
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
    addServer: (state: Draft<ServerReducerState>, action: PayloadAction<ServerState>) => {
      const server = action.payload;

      let laoState = {
        byAddress: {} as {
          [address: string]: ServerState;
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

    updateServer: (state: Draft<ServerReducerState>, action: PayloadAction<ServerState>) => {
      const updatedServer = action.payload;

      if (!(updatedServer.laoId in state.byLaoId)) {
        return;
      }

      if (!(updatedServer.address in state.byLaoId[updatedServer.laoId].byAddress)) {
        return;
      }

      // delete old frontend-backend key mapping entry
      delete state.byLaoId[updatedServer.laoId].backendKeyByFrontendKey[
        state.byLaoId[updatedServer.laoId].byAddress[updatedServer.address].frontendPublicKey
      ];

      // update byAddress entry
      state.byLaoId[updatedServer.laoId].byAddress[updatedServer.address] = updatedServer;

      // add new frontend-backend key mapping entry
      state.byLaoId[updatedServer.laoId].backendKeyByFrontendKey[updatedServer.frontendPublicKey] =
        updatedServer.serverPublicKey;
    },

    removeServer: (state, action: PayloadAction<{ laoId: string; address: string }>) => {
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
  laoId: string,
  address: ServerAddress,
  state: any,
): PublicKey | undefined => {
  const serverState = getServerState(state);

  if (laoId in serverState.byLaoId && address in serverState.byLaoId[laoId].byAddress) {
    return new PublicKey(serverState.byLaoId[laoId].byAddress[address].serverPublicKey);
  }

  return undefined;
};

const sGetServerState = (state: any) => getServerState(state);
const sGetLaoState = (state: any) => getLaosState(state);
const sGetServersByLaoId = (state: any) => getServerState(state).byLaoId;

/**
 * A function that creates a selector that retrieve the public key of the lao organizer's backend
 * @param laoId The lao id
 * @returns The public key of the lao organizer's backend and undefined if there is none
 */
export const makeLaoOrganizerBackendPublicKeySelector = (laoId: string) =>
  createSelector(
    // First input: The server state
    sGetServerState,
    // Second input: The laos state
    sGetLaoState,
    // Selector: returns the server object associated to the given address
    (serverState: ServerReducerState, laoState: LaoReducerState): PublicKey | undefined => {
      // if there is no current lao, return undefined
      if (!(laoId in laoState.byId) || !(laoId in serverState.byLaoId)) {
        return undefined;
      }

      const currentLaoState = laoState.byId[laoId];

      if (currentLaoState.organizer in serverState.byLaoId[laoId].backendKeyByFrontendKey) {
        return new PublicKey(
          serverState.byLaoId[laoId].backendKeyByFrontendKey[currentLaoState.organizer],
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
export const makeServerSelector = (laoId: string, address: ServerAddress) =>
  createSelector(
    // First input: map of lao ids to servers
    sGetServersByLaoId,
    // Selector: returns the server object associated to the given address
    (byLaoId: ServerReducerState['byLaoId']): LaoServer | undefined => {
      if (laoId in byLaoId && address in byLaoId[laoId].byAddress) {
        return LaoServer.fromState(byLaoId[laoId].byAddress[address]);
      }

      return undefined;
    },
  );
