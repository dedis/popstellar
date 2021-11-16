import { ChirpState } from 'model/objects/Chirp';
import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Hash } from 'model/objects';

/**
 * Stores all the Social Media related content
 */

// Stores all Social Media related information for a given LAO
interface SocialReducerState {

  // Stores all chirps for a given LAO
  allChirps: ChirpState[];
}

// Root state for the Social Reducer
interface SocialLaoReducerState {

  // Associates a given LAO ID with the whole representation of its social media
  byLaoId: Record<string, SocialReducerState>
}

const initialState: SocialLaoReducerState = {
  byLaoId: {
    myLaoId: {
      allChirps: [],
    },
  },
}

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
            allChirps: [],
          };
        }

        state.byLaoId[laoId].allChirps.push(chirp);
        console.log(`New chirp added:\n\tSender: ${chirp.sender}\n\tMessage: ${chirp.text}`);
      }
    }
  }
});

export const {
  addChirp
} = socialSlice.actions;

export const { reducer } = socialSlice;

export default {
  [socialReducerPath]: socialSlice.reducer,
}

export const getSocialState = (state: any): SocialLaoReducerState => state[socialReducerPath]
