import { ChirpState } from 'model/objects/Chirp';
import { createSelector, createSlice, PayloadAction } from '@reduxjs/toolkit';

/**
 * Stores all the Social Media related content
 */

interface SocialReducerState {
  // Stores all the chirps that are sent
  allChirps: ChirpState[]
}

const initialState: SocialReducerState = {
  allChirps: [],
}

const socialReducerPath = 'social';

const socialSlice = createSlice({
  name: socialReducerPath,
  initialState,
  reducers: {
    addChirp: (state, action: PayloadAction<{ chirpState: ChirpState; }>) => {
      const { chirpState } = action.payload;
      state.allChirps.push(chirpState);
    }
  }
});

export const getChirpsList = () => createSelector(
  return allChirps.map((state: ChirpState) => { state.sender, state.text});
)

export const {
  addChirp
} = socialSlice.actions;

export default {
  [socialReducerPath]: socialSlice.reducer,
}

export const getChirps = (state: any): SocialReducerState => state[socialReducerPath]
