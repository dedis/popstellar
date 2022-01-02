import { ChirpState } from 'model/objects/Chirp';
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Hash } from 'model/objects';
import { getLaosState } from './LaoReducer';
import { ReactionState } from 'model/objects/Reaction';

/**
 * Stores all the Social Media related content
 */

// Stores all Social Media related information for a given LAO
interface SocialReducerState {

  // stores all chirps id in order from the newest to the oldest
  allIdsInOrder: string[],
  // maps a chirpId to its ChirpState
  byId: Record<string, ChirpState>,
  // maps a sender to the list of ChirpIds he sent
  byUser: Record<string, string[]>,
  // maps a chirpId to the pair of the reaction_codepoint and the list of userPublicKeys
  reactionsByChirp: Record<string, Record<string, Set<string>>>,
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
      reactionsByChirp: {},
    },
  },
};

const socialReducerPath = 'social';

// helper function to find where to insert the new chirp in ascending time order
function findInsertIdx(
  array: string[], byId: Record<string, ChirpState>, element: number,
): number {
  let left: number = 0;
  let right: number = array.length;

  while (left < right) {
    const mid = Math.floor((right + left) / 2);
    if (byId[array[mid]].time > element) {
      left = mid + 1;
    } else {
      right = mid;
    }
  }
  return left;
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
            reactionsByChirp: {},
          };
        }

        const store = state.byLaoId[laoId];

        if (!store.byId[chirp.id]) {
          store.byId[chirp.id] = chirp;

          const insertIdxInAll = findInsertIdx(
            store.allIdsInOrder, store.byId, chirp.time,
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

    // Add reactions to a chirp
    addReaction: {
      prepare(laoId: Hash | string, reaction: ReactionState): any {
        return { payload: { laoId: laoId.valueOf(), reaction: reaction } };
      },
      reducer(state, action: PayloadAction<{
        laoId: string,
        reaction: ReactionState,
      }>) {
        const { laoId, reaction } = action.payload;

        if (!(laoId in state.byLaoId)) {
          return;
        }

        const store = state.byLaoId[laoId];
        if (!store.reactionsByChirp[reaction.chirp_id]) {
          store.reactionsByChirp[reaction.chirp_id] =
            {[reaction.codepoint]: new Set<string>([reaction.sender])};
        } else if (!store.reactionsByChirp[reaction.chirp_id][reaction.codepoint]) {
          store.reactionsByChirp[reaction.chirp_id][reaction.codepoint] =
            new Set<string>([reaction.sender]);
        } else {
          store.reactionsByChirp[reaction.chirp_id][reaction.codepoint].add(reaction.sender);
        }
      }
    }
  },
});

export const {
  addChirp, addReaction,
} = socialSlice.actions;

export const socialReduce = socialSlice.reducer;

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
