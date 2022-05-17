/**
 * This error is disabled since reducers use the createSlice function, which requires the user to
 * param-reassign. Please do not disable other errors.
 */
/* eslint-disable no-param-reassign */
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

/**
 * This file represents the reducer for the wallet.
 * Its job is to store the wallet state, which is represented by the wallet's encrypted seed.
 */
export interface WalletReducerState {
  seed?: string;
  mnemonic?: string;
}

const initialState: WalletReducerState = {
  seed: undefined,
  mnemonic: undefined,
};

/* Name of wallet slice in storage */
export const WALLET_REDUCER_PATH = 'wallet';

/* The store slice in charge of the wallet state */
const walletSlice = createSlice({
  name: WALLET_REDUCER_PATH,
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
  [WALLET_REDUCER_PATH]: walletSlice.reducer,
};

export const getWalletState = (state: any): WalletReducerState => state[WALLET_REDUCER_PATH];
