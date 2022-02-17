import { createSlice, PayloadAction } from '@reduxjs/toolkit';

/**
 * This file represents the reducer for the wallet.
 * Its job is to store the wallet state.
 * The wallet state is represented by the wallet's encrypted seed.
 */
interface WalletReducerState {
  seed?: string;
  mnemonic?: string;
}

const initialState: WalletReducerState = {
  seed: undefined,
  mnemonic: undefined,
};

/* name of wallet slice in storage */
const walletReducerPath = 'wallet';

/* the store slice in charge of the wallet state */
const walletSlice = createSlice({
  name: walletReducerPath,
  initialState,
  reducers: {
    setWallet: (state, action: PayloadAction<WalletReducerState>) => {
      if (!action.payload || !action.payload.seed) {
        console.debug('Wallet storage was set to: null');
        state.seed = undefined;
        state.mnemonic = undefined;
        return;
      }

      state.seed = action.payload.seed;
      state.mnemonic = action.payload.mnemonic;
      console.debug('Wallet storage was updated with new seed');
    },

    clearWallet: (state) => {
      state.seed = undefined;
      state.mnemonic = undefined;
      console.debug('Wallet storage was cleared');
    },
  },
});

export const { setWallet, clearWallet } = walletSlice.actions;

export const walletReduce = walletSlice.reducer;

export default {
  [walletReducerPath]: walletSlice.reducer,
};

export const getWalletState = (state: any): WalletReducerState => state[walletReducerPath];
