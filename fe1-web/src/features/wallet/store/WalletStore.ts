import base64url from 'base64url';

import * as platformCrypto from 'core/platform/crypto';
import { AsyncDispatch, getStore } from 'core/redux';

import { clearWallet, getWalletState, setWallet } from '../reducer';

/**
 * Encrypts Uint8Array plaintext into a ciphertext (string).
 *
 * @param plaintext - The plaintext to encrypt
 * @private
 */
async function encrypt(plaintext: Uint8Array): Promise<string> {
  const encrypted = await platformCrypto.encrypt(plaintext);
  return base64url.encode(Buffer.from(encrypted));
}

/**
 * Decrypts ciphertext (string) into a Uint8Array plaintext.
 *
 * @param cipher - The ciphertext to decrypt
 * @private
 */
async function decrypt(cipher: string): Promise<Uint8Array> {
  const decoded = base64url.toBuffer(cipher);
  const plaintext = await platformCrypto.decrypt(decoded);
  return new Uint8Array(plaintext);
}

export namespace WalletStore {
  /**
   * Stores wallet seed & associated mnemonic.
   *
   * @param mnemonic - The 12-word mnemonic to be saved
   * @param seed - The unencrypted seed
   * @returns a Promise which completes after storage
   */
  export async function store(mnemonic: string, seed: Uint8Array) {
    const binaryMnemonic: Uint8Array = new TextEncoder().encode(mnemonic);
    await getStore().dispatch(async (dispatch: AsyncDispatch): Promise<void> => {
      const encryptedSeed = await encrypt(seed);
      const encryptedMnemonic = await encrypt(binaryMnemonic);
      dispatch(
        setWallet({
          seed: encryptedSeed,
          mnemonic: encryptedMnemonic,
        }),
      );
    });
  }

  /**
   * Retrieves the wallet seed mnemonic from the store.
   *
   * @returns the mnemonic
   * @throws an error if the seed was never initialized
   */
  export async function getMnemonic(): Promise<string> {
    const cipher = getWalletState(getStore().getState()).mnemonic;
    if (!cipher) {
      throw new Error('No wallet in redux storage, insert 12-word mnemonic to backup your wallet.');
    }

    const binaryMnemonic: Uint8Array = await decrypt(cipher);
    return new TextDecoder('utf-8').decode(binaryMnemonic);
  }

  /**
   * Retrieves the wallet seed from the store.
   *
   * @returns the wallet seed
   * @throws an error if the seed was never initialized
   */
  export async function getSeed(): Promise<Uint8Array> {
    const cipher = getWalletState(getStore().getState()).seed;
    if (!cipher) {
      throw new Error('No wallet in redux storage, insert 12-word mnemonic to backup your wallet.');
    }

    return decrypt(cipher);
  }

  /**
   * Indicates whether a seed is present in the store.
   * @returns true if a seed exists, false otherwise
   */
  export function hasSeed(): boolean {
    const { seed } = getWalletState(getStore().getState());
    return seed !== undefined;
  }

  /**
   * Clears the wallet.
   */
  export function clear(): void {
    getStore().dispatch(clearWallet());
  }
}
