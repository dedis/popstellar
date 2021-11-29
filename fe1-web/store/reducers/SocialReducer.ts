import { ChirpState } from 'model/objects/Chirp';
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Hash } from 'model/objects';
import { getLaosState } from './LaoReducer';
import AVLTree, {IAVLTree} from 'avl-bst';

/**
 * Stores all the Social Media related content
 */
interface TimeChirps {
  time: number;
  chirps: string[];
}

// Stores all Social Media related information for a given LAO
interface SocialReducerState {

  // Stores all chirps for a given LAO
  // allChirps: ChirpState[],
  byTime: IAVLTree<number, TimeChirps>
  // byId maps a chirpId to the ChirpState
  byId: Record<string, ChirpState>,
  // byUser maps an sender to the list of ChirpId he sent
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
      //allChirps: [],
      byTime: AVLTree.create<number, TimeChirps>((timeState) => timeState.time),
      byId: {},
      byUser: {},
    },
  },
};

const socialReducerPath = 'social';

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
            //allChirps: [],
            byTime: AVLTree.create<number, TimeChirps>((timeState) => timeState.time),
            byId: {},
            byUser: {},
          };
        }

        if (!state.byLaoId[laoId].byId[chirp.id]) {
          state.byLaoId[laoId].byId[chirp.id] = chirp;

          const node = state.byLaoId[laoId].byTime.search(chirp.time);
          if (node === null) {
            state.byLaoId[laoId].byTime.insert({ time: chirp.time, chirps: [chirp.id]});
          } else {
            node.chirps.push(chirp.id);
          }

          if (!state.byLaoId[laoId].byUser[chirp.sender]) {
            state.byLaoId[laoId].byUser[chirp.sender] = [chirp.id];
          } else {
            // TODO: add chirp ID to the sender's chirp list at the right position
          }


        }


        /*state.byLaoId[laoId].allChirps.unshift(chirp);
        console.log(`New chirp added:\n\tSender: ${chirp.sender}\n\tMessage: ${chirp.text}`);*/
      },
    },
  },
});

export const {
  addChirp,
} = socialSlice.actions;

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
      //return chirpList.byLaoId[laoId].allChirps;
      const allChirps: ChirpState[] = [];
      chirpList.byLaoId[laoId].byTime.forEach(e => {
        e.chirps.forEach(id => allChirps.unshift(chirpList.byLaoId[laoId].byId[id]))
      });
      return allChirps;
    }
    return [];
  },
);
