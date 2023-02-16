import * as bip39 from 'bip39';
import * as Random from 'expo-random';

import { WalletStore } from '../store';

/**
 * Generates a new BIP-39 mnemonic.
 *
 * @returns a string containing a new generated 12-word mnemonic
 */
export function generateMnemonicSeed() {
  return bip39.generateMnemonic(
    undefined,
    // unsafe cast but it works, the interface is similar enough fortunately..
    Random.getRandomBytes as unknown as (size: number) => Buffer,
  );
}

/**
 * Initializes the wallet with the given BIP-39 mnemonic.
 *
 * @param mnemonic - The 12-word representation of the wallet seed
 * @return a Promise that completes when the wallet seed has been stored
 * @throws an Error if the mnemonic is invalid
 */
export async function importMnemonic(mnemonic: string): Promise<void> {
  if (mnemonic === undefined) {
    throw new Error('Error while initializing the wallet seed: seed is undefined');
  }

  const seedIsValid: boolean = bip39.validateMnemonic(mnemonic.toLowerCase());
  if (!seedIsValid) {
    throw new Error('Error while initializing the wallet seed: seed is invalid');
  }

  const seed: Uint8Array = await bip39.mnemonicToSeed(mnemonic.toLowerCase());
  await WalletStore.store(mnemonic, seed);
}

/**
 * Exports the wallet's mnemonic.
 *
 * @return a Promise<string> that resolves to the mnemonic
 * @throws an Error if no mnemonic has been saved in the store
 */
export function exportMnemonic(): Promise<string> {
  return WalletStore.getMnemonic();
}

/**
 * Forgets about the wallet, wiping out any trace of it.
 */
export function forget() {
  WalletStore.clear();
}
