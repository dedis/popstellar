import {
  createSlice, createSelector, PayloadAction, Draft,
} from '@reduxjs/toolkit';
import { Hash, Lao, LaoState } from 'model/objects';
import laosData from 'res/laoData';

/**
 * Reducer & associated function implementation to store all known LAOs
 * and a reference to the current open one.
 */

interface LaoReducerState {
  byId: Record<string, LaoState>,
  allIds: string[],
  currentId?: string,
}

const initialState: LaoReducerState = {
  byId: Object.assign({},
    ...laosData.map((lao: Lao) => ({
      [lao.id.toString()]: lao.toState(),
    }))),
  allIds: laosData.map((lao) => lao.id.valueOf()),
};

const addLaoReducer = (state: Draft<LaoReducerState>, action: PayloadAction<LaoState>) => {
  const newLao = action.payload;

  if (!(newLao.id in state.byId)) {
    state.byId[newLao.id] = newLao;
    state.allIds.push(newLao.id);
  }
};

const laoReducerPath = 'laos';
const laosSlice = createSlice({
  name: laoReducerPath,
  initialState,
  reducers: {
    // Add a LAO to the list of known LAOs
    addLao: addLaoReducer,

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
  },
});

export const {
  addLao, removeLao, clearAllLaos, connectToLao, disconnectFromLao,
} = laosSlice.actions;

export const getLaosState = (state: any): LaoReducerState => state[laoReducerPath];

export function makeCurrentLao() {
  return createSelector(
    // First input: all LAOs map
    (state) => getLaosState(state).byId,
    // Second input: current LAO id
    (state) => getLaosState(state).currentId,
    // Selector: returns a LaoState -- should it return a Lao object?
    (laoMap: Record<string, LaoState>, currentId: string | undefined) : Lao | undefined => {
      if (currentId === undefined || !(currentId in laoMap)) {
        return undefined;
      }

      return Lao.fromState(laoMap[currentId]);
    },
  );
}

export const makeLaosList = () => createSelector(
  // First input: all LAOs map
  (state) => getLaosState(state).byId,
  // Second input: sorted LAO ids list
  (state) => getLaosState(state).allIds,
  // Selector: returns an array of LaoStates -- should it return an array of Lao objects?
  (laoMap: Record<string, LaoState>, laoIds: string[]) : Lao[] => laoIds
    .map((id) => Lao.fromState(laoMap[id])),
);

export default {
  [laoReducerPath]: laosSlice.reducer,
};
