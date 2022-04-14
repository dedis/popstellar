/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';

import { PublicKey, Server, ServerAddress, ServerState } from 'core/objects';

/**
 * Reducer & associated function implementation to store all known servers
 */

export interface ServerReducerState {
  byAddress: Record<ServerAddress, ServerState>;
  allAddresses: ServerAddress[];
}

const initialState: ServerReducerState = {
  byAddress: {},
  allAddresses: [],
};

export const SERVER_REDUCER_PATH = 'servers';

const serverSlice = createSlice({
  name: SERVER_REDUCER_PATH,
  initialState,
  reducers: {
    addServer: (state: Draft<ServerReducerState>, action: PayloadAction<ServerState>) => {
      const server = action.payload;

      state.byAddress[server.address] = server;
      state.allAddresses.push(server.address);
    },

    updateServer: (state: Draft<ServerReducerState>, action: PayloadAction<ServerState>) => {
      const updatedServer = action.payload;

      if (!(updatedServer.address in state.byAddress)) {
        return;
      }

      state.byAddress[updatedServer.address] = updatedServer;
    },

    removeServer: (state, action: PayloadAction<string>) => {
      const serverAddress = action.payload;

      if (serverAddress in state.byAddress) {
        delete state.byAddress[serverAddress];
        state.allAddresses = state.allAddresses.filter((id) => id !== serverAddress);
      }
    },

    clearAllServers: (state) => {
      state.byAddress = {};
      state.allAddresses = [];
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
 * A function to directly retrieve the public key from the redux store.
 * @remark NOTE: This function does not memoize the result. If you need this, use makeServerSelector instead
 * @param address The server address
 * @param state The redux state
 * @returns The public key for the given server address or undefined if there is none
 */
export const getServerPublicKeyByAddress = (
  address: ServerAddress,
  state: any,
): PublicKey | undefined => {
  const serverState = getServerState(state);

  if (address in serverState.byAddress) {
    return new PublicKey(serverState.byAddress[address].publicKey);
  }

  return undefined;
};

/**
 * Creates a server selector for a given address. Can for example be used in useSelector()
 * @param address The server address
 * @returns A selector for the server object for the given address or undefined if there is none
 */
export const makeServerSelector = (address: ServerAddress) =>
  createSelector(
    // First input: map of addresses to servers
    (state) => getServerState(state).byAddress,
    // Selector: returns the server object associated to the given address
    (byAddress: Record<string, ServerState>): Server | undefined =>
      address in byAddress ? Server.fromState(byAddress[address]) : undefined,
  );
