/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, Draft, PayloadAction } from '@reduxjs/toolkit';
import { REHYDRATE, RehydrateAction } from 'redux-persist';

import { getKeyPairState } from 'core/keypair';
import { Hash } from 'core/objects';

import { Lao, LaoState } from '../objects';

/**
 * Reducer & associated function implementation to store all known LAOs
 * and a reference to the current open one.
 */

export interface LaoReducerState {
  byId: Record<string, LaoState>;
  allIds: string[];
  currentId?: string;
}

const initialState: LaoReducerState = {
  byId: {},
  allIds: [],
};

const addLaoReducer = (state: Draft<LaoReducerState>, action: PayloadAction<LaoState>) => {
  const newLao = action.payload;

  if (newLao.id in state.byId) {
    // we already have some data on this lao stored
    // merge server addresses
    state.byId[newLao.id].server_addresses = [
      // the way via a set guarantees the list does not contain duplicates
      ...new Set([...state.byId[newLao.id].server_addresses, ...newLao.server_addresses]),
    ];
  } else {
    state.byId[newLao.id] = newLao;
    state.allIds.push(newLao.id);
  }
};

export const LAO_REDUCER_PATH = 'laos';

const laosSlice = createSlice({
  name: LAO_REDUCER_PATH,
  initialState,
  reducers: {
    // Add a LAO to the list of known LAOs
    addLao: addLaoReducer,

    // Update a LAO
    updateLao: (state: Draft<LaoReducerState>, action: PayloadAction<LaoState>) => {
      const updatedLao = action.payload;

      if (!(updatedLao.id in state.byId)) {
        return;
      }

      state.byId[updatedLao.id] = updatedLao;
    },

    // Remove a LAO to the list of known LAOs
    removeLao: (state, action: PayloadAction<Hash>) => {
      const laoId = action.payload.valueOf();

      if (laoId in state.byId) {
        delete state.byId[laoId];
        state.allIds = state.allIds.filter((id) => id !== laoId);
      }
    },

    // Empty the list of known LAOs ("reset")
    clearAllLaos: (state) => {
      if (state.currentId === undefined) {
        state.byId = {};
        state.allIds = [];
      }
    },

    // Connect to a LAO for a given ID
    // Warning: this action is only accepted if we are not already connected to a LAO
    setCurrentLao: (state, action: PayloadAction<LaoState>) => {
      addLaoReducer(state, action);

      if (state.currentId === undefined) {
        const lao = action.payload;
        state.currentId = lao.id;
      }
    },

    // Disconnected from the current LAO (idempotent)
    clearCurrentLao: (state) => {
      state.currentId = undefined;
    },

    // Add a LAO server address
    addLaoServerAddress: {
      prepare(laoId: Hash | string, serverAddress: string) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            serverAddress,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          serverAddress: string;
        }>,
      ) {
        const { laoId, serverAddress } = action.payload;

        // Lao not initialized, return
        if (!(laoId in state.byId)) {
          return;
        }

        // if not already in the list, add the new address
        if (!state.byId[laoId].server_addresses.find((a) => a === serverAddress)) {
          state.byId[laoId].server_addresses.push(serverAddress);
        }
      },
    },

    // Add a subscribed to channel
    addSubscribedChannel: {
      prepare(laoId: Hash | string, channel: string) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            channel,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          channel: string;
        }>,
      ) {
        const { laoId, channel } = action.payload;

        // Lao not initialized, return
        if (!(laoId in state.byId)) {
          return;
        }

        // if not already in the list, add the new channel
        if (!state.byId[laoId].subscribed_channels.find((a) => a === channel)) {
          state.byId[laoId].subscribed_channels.push(channel);
        }
      },
    },

    // Add a subscribed to channel
    removeSubscribedChannel: {
      prepare(laoId: Hash | string, channel: string) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            channel,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          channel: string;
        }>,
      ) {
        const { laoId, channel } = action.payload;

        // Lao not initialized, return
        if (!(laoId in state.byId)) {
          return;
        }

        state.byId[laoId].subscribed_channels = state.byId[laoId].subscribed_channels.filter(
          (a) => a !== channel,
        );
      },
    },

    // Update the last roll call observed in the LAO and for which we have a token
    setLaoLastRollCall: {
      prepare(laoId: Hash | string, rollCallId: Hash | string, hasToken: boolean): any {
        return {
          payload: {
            laoId: laoId.valueOf(),
            rollCallId: rollCallId.valueOf(),
            hasToken: hasToken,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          rollCallId: string;
          hasToken: boolean;
        }>,
      ) {
        const { laoId, rollCallId, hasToken } = action.payload;

        // Lao not initialized, return
        if (!(laoId in state.byId)) {
          return;
        }

        state.byId[laoId].last_roll_call_id = rollCallId;
        if (hasToken) {
          state.byId[laoId].last_tokenized_roll_call_id = rollCallId;
        }
      },
    },
  },
  extraReducers: (builder) => {
    // this is called by the persistence layer of Redux, upon starting the application
    builder.addCase(REHYDRATE, (state, rehydrateAction: RehydrateAction) => {
      if (!rehydrateAction.payload || !(LAO_REDUCER_PATH in rehydrateAction.payload)) {
        return state;
      }

      const payload = rehydrateAction.payload as {
        [LAO_REDUCER_PATH]: LaoReducerState;
      };

      return {
        ...state,
        ...(LAO_REDUCER_PATH in rehydrateAction.payload ? payload[LAO_REDUCER_PATH] : {}),
        // make sure we always start disconnected
        currentId: undefined,
      };
    });
  },
});

export const {
  addLao,
  updateLao,
  removeLao,
  clearAllLaos,
  setCurrentLao,
  clearCurrentLao,
  setLaoLastRollCall,
  addLaoServerAddress,
  addSubscribedChannel,
  removeSubscribedChannel,
} = laosSlice.actions;

export const getLaosState = (state: any): LaoReducerState => state[LAO_REDUCER_PATH];

// Selector helper functions
const sGetCurrentLaoId = (state: any) => getLaosState(state).currentId;
const sGetLaosById = (state: any) => getLaosState(state).byId;
const sGetAllIds = (state: any) => getLaosState(state).allIds;
const sGetId = (id: string | undefined) => () => id;
const sGetUserPublicKey = (state: any) => getKeyPairState(state)?.keyPair?.publicKey;

export function makeLao() {
  return createSelector(
    // First input: all LAOs map
    sGetLaosById,
    // Second input: current LAO id
    sGetCurrentLaoId,
    // Selector: returns a LaoState -- should it return a Lao object?
    (laoMap: Record<string, LaoState>, currentId: string | undefined): Lao | undefined => {
      if (currentId === undefined || !(currentId in laoMap)) {
        return undefined;
      }

      return Lao.fromState(laoMap[currentId]);
    },
  );
}

/**
 * Gets a lao by its id or returns undefined if there is none with the given id
 * @remark This function does not memoize its results, only use it outside of react components
 * @param laoId The id of the lao
 * @param state The redux state
 */
export const getLaoById = (laoId: string, state: unknown) => {
  const laoMap = getLaosState(state).byId;

  if (!(laoId in laoMap)) {
    return undefined;
  }

  return Lao.fromState(laoMap[laoId]);
};

/**
 * Shorthand selector for widely used variant of makeLao()
 * Selects the current lao from the redux store
 * @returns The current lao
 */
export const selectCurrentLao = makeLao();

export const selectCurrentLaoId = createSelector(
  // First input: current LAO id
  sGetCurrentLaoId,
  (currentId: string | undefined): Hash | undefined =>
    currentId ? new Hash(currentId) : undefined,
);

export const selectLaoIdToNameMap = createSelector(
  // First input: current LAO id
  sGetLaosById,
  (byId: Record<string, LaoState>): Record<string, string> =>
    Object.keys(byId).reduce((obj, laoId) => {
      obj[laoId] = byId[laoId].name;
      return obj;
    }, {} as Record<string, string>),
);

export const selectLaoIdsList = createSelector(
  // Input: sorted LAO ids list
  sGetAllIds,
  // Selector: returns an array of LaoIDs
  (laoIds: string[]): Hash[] => laoIds.map((laoId) => new Hash(laoId)),
);

export const selectLaosList = createSelector(
  // First input: all LAOs map
  sGetLaosById,
  // Second input: sorted LAO ids list
  sGetAllIds,
  // Selector: returns an array of LaoStates -- should it return an array of Lao objects?
  (laoMap: Record<string, LaoState>, laoIds: string[]): Lao[] =>
    laoIds.map((id) => Lao.fromState(laoMap[id])),
);

export const selectLaosMap = createSelector(
  // First input: all LAOs map
  sGetLaosById,
  // Selector: returns an array of LaoStates -- should it return an array of Lao objects?
  (laoMap: Record<string, LaoState>): Record<string, Lao> =>
    Object.keys(laoMap).reduce((acc, id) => {
      acc[id] = Lao.fromState(laoMap[id]);
      return acc;
    }, {} as Record<string, Lao>),
);

/**
 * Creates a selector that returns whether the current user is an organizer
 * of the given lao. Defaults to the current lao.
 * @param laoId Id of the lao the selector should be created for
 */
export const makeIsLaoOrganizerSelector = (laoId?: string) =>
  createSelector(
    // First input: all LAOs map
    sGetLaosById,
    // Second input: current LAO id
    laoId ? sGetId(laoId) : sGetCurrentLaoId,
    // Third input: the public key of the user
    sGetUserPublicKey,
    // Selector: returns whether the user is an organizer of the current lao
    (
      laoMap: Record<string, LaoState>,
      selectedLaoId: string | undefined,
      pKey: string | undefined,
    ): boolean => !!selectedLaoId && laoMap[selectedLaoId]?.organizer === pKey,
  );

export const selectIsLaoOrganizer = makeIsLaoOrganizerSelector();

/**
 * Creates a selector that returns whether the current user is a witness
 * of the current lao.
 */
export const makeIsLaoWitnessSelector = (laoId?: string) =>
  createSelector(
    // First input: all LAOs map
    sGetLaosById,
    // Second input: current LAO id
    laoId ? sGetId(laoId) : sGetCurrentLaoId,
    // Third input: the public key of the user
    sGetUserPublicKey,
    // Selector: returns whether the user is a witness of the current lao
    (
      laoMap: Record<string, LaoState>,
      selectedLaoId: string | undefined,
      pKey: string | undefined,
    ): boolean => !!(selectedLaoId && pKey) && laoMap[selectedLaoId]?.witnesses.includes(pKey),
  );

export const selectIsLaoWitness = makeIsLaoWitnessSelector();

export const laoReduce = laosSlice.reducer;

export default {
  [LAO_REDUCER_PATH]: laosSlice.reducer,
};
