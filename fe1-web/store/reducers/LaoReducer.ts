import { createSlice, createSelector, PayloadAction } from '@reduxjs/toolkit';
import { Lao, LaoState } from 'model/objects';
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

const laosSlice = createSlice({
  name: 'laos',
  initialState,
  reducers: {
    laoAdded: (state, action: PayloadAction<LaoState>) => {
      const newLao = action.payload;
      state.byId[newLao.id] = newLao;
      state.allIds.push(newLao.id);
    },
    laoRemoved: (state, action: PayloadAction<string>) => {
      const laoId = action.payload;
      delete state.byId[laoId];
      state.allIds = state.allIds.filter((id) => id !== laoId);
    },
    laosCleared: (state) => {
      if (state.currentId === undefined) {
        state.byId = {};
        state.allIds = [];
      }
    },
    laoConnected: (state, action: PayloadAction<string>) => {
      const laoId = action.payload;
      state.currentId = (laoId in state.byId) ? laoId : undefined;
    },
    laoDisconnected: (state) => {
      state.currentId = undefined;
    },
  },
});

export const {
  laoAdded, laoRemoved, laosCleared, laoConnected, laoDisconnected,
} = laosSlice.actions;

export const makeCurrentLao = () => createSelector(
  // First input: all LAOs map
  (state: LaoReducerState) => state.byId,
  // Second input: current LAO id
  (state: LaoReducerState) => state.currentId,
  // Selector: returns a LaoState -- should it return a Lao object?
  (laoMap: Record<string, LaoState>, currentId: string | undefined) => {
    if (currentId === undefined || !(currentId in laoMap)) {
      return undefined;
    }

    return laoMap[currentId];
  },
);

export const makeLaosList = () => createSelector(
  // First input: all LAOs map
  (state: LaoReducerState) => state.byId,
  // Second input: sorted LAO ids list
  (state: LaoReducerState) => state.allIds,
  // Selector: returns an array of LaoStates -- should it return an array of Lao objects?
  (laoMap: Record<string, LaoState>, laoIds: string[]) => laoIds
    .map((id) => laoMap[id]),
);

export default laosSlice.reducer;
