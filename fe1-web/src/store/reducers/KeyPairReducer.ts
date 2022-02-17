import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { KeyPairState } from 'model/objects';

/**
 * Reducer to store a set of public/private key
 * This might have to be extended as part of the Digital Wallet project
 */

interface KeyPairReducerState {
  keyPair?: KeyPairState;
}

const initialState: KeyPairReducerState = {
  keyPair: undefined,
};

const keyPairReducerPath = 'keyPairs';
const keyPairsSlice = createSlice({
  name: keyPairReducerPath,
  initialState,
  reducers: {
    // Set global key pair
    setKeyPair: (state, action: PayloadAction<KeyPairState>) => {
      if (!action.payload) {
        console.log('KeyPair storage was set to: null');
        state.keyPair = undefined;
      }

      state.keyPair = action.payload;
      console.log(`KeyPair storage was updated with public key: ${state.keyPair.publicKey.toString()}`);
    },
  },
});

export const {
  setKeyPair,
} = keyPairsSlice.actions;

export const keyPairReduce = keyPairsSlice.reducer;

export default {
  [keyPairReducerPath]: keyPairsSlice.reducer,
};

export const getKeyPairState = (state: any): KeyPairReducerState => state[keyPairReducerPath];
