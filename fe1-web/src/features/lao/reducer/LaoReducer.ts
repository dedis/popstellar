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
    connectToLao: (state, action: PayloadAction<LaoState>) => {
      addLaoReducer(state, action);

      if (state.currentId === undefined) {
        const lao = action.payload;
        state.currentId = lao.id;
      }
    },

    // Disconnected from the current LAO (idempotent)
    disconnectFromLao: (state) => {
      state.currentId = undefined;
    },

    // Set the LAO server address
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
  connectToLao,
  disconnectFromLao,
  setLaoLastRollCall,
  addLaoServerAddress,
} = laosSlice.actions;

export const getLaosState = (state: any): LaoReducerState => state[LAO_REDUCER_PATH];

export function makeLao(id: string | undefined = undefined) {
  return createSelector(
    // First input: all LAOs map
    (state) => getLaosState(state).byId,
    // Second input: current LAO id
    (state) => id || getLaosState(state).currentId,
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
  (state) => getLaosState(state).currentId,
  (currentId: string | undefined): Hash | undefined =>
    currentId ? new Hash(currentId) : undefined,
);

export const selectLaoIdToNameMap = createSelector(
  // First input: current LAO id
  (state) => getLaosState(state).byId,
  (byId: Record<string, LaoState>): Record<string, string> =>
    Object.keys(byId).reduce((obj, laoId) => {
      obj[laoId] = byId[laoId].name;
      return obj;
    }, {} as Record<string, string>),
);

export const selectLaoIdsList = createSelector(
  // Input: sorted LAO ids list
  (state) => getLaosState(state).allIds,
  // Selector: returns an array of LaoIDs
  (laoIds: string[]): Hash[] => laoIds.map((laoId) => new Hash(laoId)),
);

export const selectLaosList = createSelector(
  // First input: all LAOs map
  (state) => getLaosState(state).byId,
  // Second input: sorted LAO ids list
  (state) => getLaosState(state).allIds,
  // Selector: returns an array of LaoStates -- should it return an array of Lao objects?
  (laoMap: Record<string, LaoState>, laoIds: string[]): Lao[] =>
    laoIds.map((id) => Lao.fromState(laoMap[id])),
);

export const selectLaosMap = createSelector(
  // First input: all LAOs map
  (state) => getLaosState(state).byId,
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
    (state) => getLaosState(state).byId,
    // Second input: current LAO id
    (state) => laoId || getLaosState(state)?.currentId,
    // Third input: the public key of the user
    (state) => getKeyPairState(state)?.keyPair?.publicKey,
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
 * of the given lao. Defaults to the current lao.
 * @param laoId Id of the lao the selector should be created for
 */
export const makeIsLaoWitnessSelector = (laoId?: string) =>
  createSelector(
    // First input: all LAOs map
    (state) => getLaosState(state).byId,
    // Second input: current LAO id
    (state) => laoId || getLaosState(state)?.currentId,
    // Third input: the public key of the user
    (state) => getKeyPairState(state)?.keyPair?.publicKey,
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
