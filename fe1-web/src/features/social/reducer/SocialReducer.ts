/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

import { Hash, PublicKey, Timestamp } from 'core/objects';

import { Chirp, ChirpState, ReactionState } from '../objects';

/**
 * Stores all the Social Media related content
 */

// Stores all Social Media related information for a given LAO
interface SocialReducerState {
  // stores all chirps id in order from the newest to the oldest
  allIdsInOrder: string[];
  // maps a chirpId to its ChirpState
  byId: Record<string, ChirpState>;
  // maps a sender to the list of ChirpIds he sent
  byUser: Record<string, string[]>;
  // maps a chirpId to the pair of the reaction_codepoint and the list of userPublicKeys
  reactionsByChirp: Record<string, Record<string, string[]>>;
}

// Root state for the Social Reducer
export interface SocialLaoReducerState {
  // Associates a given LAO ID with the whole representation of its social media
  byLaoId: Record<string, SocialReducerState>;
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

/* Name of the social media slice in storage */
export const SOCIAL_REDUCER_PATH = 'social';

// helper function to find where to insert the new chirp in ascending time order
function findInsertIdx(array: string[], byId: Record<string, ChirpState>, element: number): number {
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
  name: SOCIAL_REDUCER_PATH,
  initialState,
  reducers: {
    // Add a chirp to the list of chirps
    addChirp: {
      prepare(laoId: Hash, chirp: ChirpState) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            chirp: chirp,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          chirp: ChirpState;
        }>,
      ) {
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

        // add the chirp to byId or update the chirp state if it's already in byId
        // if the chirp has not been added (didn't receive a delete request before)
        // or if the addChirp and deleteChirp of this chirp.id are sent by different sender
        // in this case we trust the sender of addChirp and keep the chirp as not deleted
        if (!(store.byId[chirp.id] && store.byId[chirp.id].sender === chirp.sender)) {
          store.byId[chirp.id] = chirp;
        }

        // even the chirp is deleted, we add it to allIdsInOrder to display the message
        const insertIdxInAll = findInsertIdx(store.allIdsInOrder, store.byId, chirp.time);
        store.allIdsInOrder.splice(insertIdxInAll, 0, chirp.id);

        if (!state.byLaoId[laoId].byUser[chirp.sender]) {
          store.byUser[chirp.sender] = [chirp.id];
        } else {
          const senderChirps = store.byUser[chirp.sender];
          const insertIdxInUser = findInsertIdx(senderChirps, store.byId, chirp.time);
          senderChirps.splice(insertIdxInUser, 0, chirp.id);
        }
      },
    },

    // Delete a chirp in the list of chirps
    deleteChirp: {
      prepare(laoId: Hash, chirp: ChirpState) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            chirp: chirp,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          chirp: ChirpState;
        }>,
      ) {
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

        // store the deleted chirp
        const displayTime = store.byId[chirp.id] ? store.byId[chirp.id].time : chirp.time;
        const deletedChirp = new Chirp({
          id: new Hash(chirp.id),
          sender: new Hash(chirp.sender),
          time: new Timestamp(displayTime),
          text: '',
          isDeleted: true,
        }).toState();
        if (
          !store.byId[chirp.id] ||
          (store.byId[chirp.id] && store.byId[chirp.id].sender === deletedChirp.sender)
        ) {
          store.byId[chirp.id] = deletedChirp;
        }
        // we ignore the case if the delete request is not sent by the original sender
      },
    },

    // Add reactions to a chirp
    addReaction: {
      prepare(laoId: Hash, reaction: ReactionState) {
        return {
          payload: {
            laoId: laoId.valueOf(),
            reaction: reaction,
          },
        };
      },
      reducer(
        state,
        action: PayloadAction<{
          laoId: string;
          reaction: ReactionState;
        }>,
      ) {
        const { laoId, reaction } = action.payload;

        if (!(laoId in state.byLaoId)) {
          state.byLaoId[laoId] = {
            allIdsInOrder: [],
            byId: {},
            byUser: {},
            reactionsByChirp: {},
          };
        }

        const store = state.byLaoId[laoId];

        if (!store.reactionsByChirp[reaction.chirpId]) {
          store.reactionsByChirp[reaction.chirpId] = { [reaction.codepoint]: [reaction.sender] };
        } else if (!store.reactionsByChirp[reaction.chirpId][reaction.codepoint]) {
          store.reactionsByChirp[reaction.chirpId][reaction.codepoint] = [reaction.sender];
        } else if (
          !store.reactionsByChirp[reaction.chirpId][reaction.codepoint].includes(reaction.sender)
        ) {
          store.reactionsByChirp[reaction.chirpId][reaction.codepoint].push(reaction.sender);
        } else {
          console.debug('The sender already reacted to this reaction');
        }
      },
    },
  },
});

export const { addChirp, deleteChirp, addReaction } = socialSlice.actions;

export const socialReduce = socialSlice.reducer;

export default {
  [SOCIAL_REDUCER_PATH]: socialSlice.reducer,
};

export const getSocialState = (state: any): SocialLaoReducerState => state[SOCIAL_REDUCER_PATH];

// Selector helper functions
const selectSocialState = (state: any) => getSocialState(state);

export const makeChirpsList = (laoId?: Hash) =>
  createSelector(
    // First input: Get all chirps across all LAOs
    selectSocialState,
    (chirpList: SocialLaoReducerState): ChirpState[] => {
      const serializedLaoId = laoId?.valueOf();

      if (!serializedLaoId) {
        return [];
      }

      if (chirpList.byLaoId[serializedLaoId]) {
        const store = chirpList.byLaoId[serializedLaoId];
        const allChirps: ChirpState[] = [];
        store.allIdsInOrder.forEach((id) => allChirps.push(store.byId[id]));
        return allChirps;
      }
      return [];
    },
  );

export const makeChirpsListOfUser = (laoId?: Hash) => (user?: PublicKey) => {
  const userPublicKey = user?.valueOf();
  return createSelector(
    // First input: Get all chirps across all LAOs
    selectSocialState,
    (chirpList: SocialLaoReducerState): ChirpState[] => {
      const serializedLaoId = laoId?.valueOf();

      if (!serializedLaoId || !userPublicKey) {
        return [];
      }

      const laoChirps = chirpList.byLaoId[serializedLaoId];

      if (laoChirps) {
        const allUserChirps: ChirpState[] = [];
        const userChirps = laoChirps.byUser[userPublicKey];
        if (userChirps) {
          userChirps.forEach((id: string) =>
            allUserChirps.push(chirpList.byLaoId[serializedLaoId].byId[id]),
          );
          return allUserChirps;
        }
      }
      return [];
    },
  );
};

const createReactionsEntry = (reactionByUser: Record<string, string[]>) => ({
  '👍': reactionByUser['👍'] ? reactionByUser['👍'].length : 0,
  '👎': reactionByUser['👎'] ? reactionByUser['👎'].length : 0,
  '❤️': reactionByUser['❤️'] ? reactionByUser['❤️'].length : 0,
});

export const makeReactionsList = (laoId?: Hash) =>
  createSelector(
    selectSocialState,
    (list: SocialLaoReducerState): Record<string, Record<string, number>> => {
      const serializedLaoId = laoId?.valueOf();

      if (!serializedLaoId) {
        return {};
      }

      if (list.byLaoId[serializedLaoId]) {
        const store = list.byLaoId[serializedLaoId];
        const reactions: Record<string, Record<string, number>> = {};
        store.allIdsInOrder.forEach((id) => {
          const chirpReactions = store.reactionsByChirp[id];
          if (chirpReactions) {
            reactions[id] = createReactionsEntry(chirpReactions);
          }
        });
        return reactions;
      }
      return {};
    },
  );
