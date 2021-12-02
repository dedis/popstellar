import { ChirpState } from 'model/objects/Chirp';
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Hash } from 'model/objects';
import { getLaosState } from './LaoReducer';

/**
 * Stores all the Social Media related content
 */

// Stores all Social Media related information for a given LAO
interface SocialReducerState {

  // stores all chirps id in order from the newest to the oldest
  allIdsInOrder: string[],
  // byId maps a chirpId to its ChirpState
  byId: Record<string, ChirpState>,
  // byUser maps a sender to the list of ChirpId he sent
  byUser: Record<string, string[]>,
}

// Root state for the Social Reducer
interface SocialLaoReducerState {

  // Associates a given LAO ID with the whole representation of its social media
  byLaoId: Record<string, SocialReducerState>
}

const initialState: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
    },
  },
};

const socialReducerPath = 'social';

// helper function to find where to insert the new chirp in ascending order
function findInsertIdx(
  array: string[], byId: Record<string, ChirpState>, element: number,
): number {
  if (array.length === 0) {
    return 0;
  }

  let left: number = 0;
  let right: number = array.length - 1;
  let mid: number = -1;
  let index: number = -1;

  while (left <= right) {
    if (byId[array[right]].time >= element) {
      index = right + 1;
      break;
    } else if (byId[array[left]].time <= element) {
      index = left;
      break;
    } else if (right - left === 1) {
      index = right;
      break;
    } else {
      mid = Math.floor((right + left) / 2);
      if (byId[array[mid]].time === element) {
        index = mid;
        break;
      }
      if (byId[array[mid]].time > element) {
        left = mid;
      } else {
        right = mid;
      }
    }
  }
  return index;
}

const socialSlice = createSlice({
  name: socialReducerPath,
  initialState,
  reducers: {

    // Add a chirp to the list of chirps
    addChirp: {
      prepare(laoId: Hash | string, chirp: ChirpState): any {
        return { payload: { laoId: laoId.valueOf(), chirp: chirp } };
      },
      reducer(state, action: PayloadAction<{
        laoId: string,
        chirp: ChirpState,
      }>) {
        const { laoId, chirp } = action.payload;

        if (!(laoId in state.byLaoId)) {
          state.byLaoId[laoId] = {
            allIdsInOrder: [],
            byId: {},
            byUser: {},
          };
        }

        const store = state.byLaoId[laoId];

        if (!store.byId[chirp.id]) {
          store.byId[chirp.id] = chirp;

          const insertIdxInAll = findInsertIdx(
            store.allIdsInOrder, store.byId, chirp.time
          );
          store.allIdsInOrder.splice(insertIdxInAll, 0, chirp.id);

          if (!state.byLaoId[laoId].byUser[chirp.sender]) {
            store.byUser[chirp.sender] = [chirp.id];
          } else {
            const senderChirps = store.byUser[chirp.sender];
            const insertIdxInUser = findInsertIdx(senderChirps, store.byId, chirp.time);
            senderChirps.splice(insertIdxInUser, 0, chirp.id);
          }
        }
      },
    },
  },
});

export const {
  addChirp,
} = socialSlice.actions;

export const socialReducer = socialSlice.reducer;

export default {
  [socialReducerPath]: socialSlice.reducer,
};

export const getSocialState = (state: any): SocialLaoReducerState => state[socialReducerPath];

export const makeChirpsList = () => createSelector(
  // First input: Get all chirps across all LAOs
  (state) => getSocialState(state),
  // Second input: Get the current LAO id,
  (state) => getLaosState(state).currentId,
  (chirpList: SocialLaoReducerState, laoId: string | undefined): ChirpState[] => {
    if (!laoId) {
      return [];
    }
    if (chirpList.byLaoId[laoId]) {
      const allChirps: ChirpState[] = [];
      chirpList.byLaoId[laoId].allIdsInOrder.forEach(
        (id) => allChirps.push(chirpList.byLaoId[laoId].byId[id]),
      );
      return allChirps;
    }
    return [];
  },
);
