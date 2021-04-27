import { createSlice, PayloadAction } from '@reduxjs/toolkit';

/**
 * This file represents the reducer for the wallet state. Its
 * job is to handle actions between the application and the
 * storage (e.g. storing the wallet seed)
 * The wallet state is represented by the encrypted wallet seed.
 */

interface WalletReducerState {
  walletState?: ArrayBuffer;
}

const initialState: WalletReducerState = {
  walletState: undefined,
};

/* name of wallet slice in storage */
const walletReducerPath = 'wallet';

/* the store slice in charge of the wallet state */
const walletSlice = createSlice({
  name: walletReducerPath,
  initialState,
  reducers: {
    /* set global wallet state */
    setWalletState: (state, action: PayloadAction<ArrayBuffer>) => {
      if (!action.payload) {
        console.log('KeyPair storage was set to: null');
        state.walletState = undefined;
      }

      state.walletState = action.payload;
      console.log(`Wallet storage was updated with new state: ${state.walletState}`);
    },
  },
});

export const {
  setWalletState,
} = walletSlice.actions;

export default {
  [walletReducerPath]: walletSlice.reducer,
};

export const getWalletState = (state: any): WalletReducerState => state[walletReducerPath];
