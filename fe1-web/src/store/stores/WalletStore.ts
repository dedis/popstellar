import base64url from 'base64url';
import platformCrypto from 'platform/crypto';
import { getWalletState, setWallet, clearWallet } from 'store/reducers';
import { AsyncDispatch, getStore } from '../Storage';

/**
 * Encrypt Uint8Array plaintext into a ciphertext (string)
 * @param plaintext
 * @private
 */
async function encrypt(plaintext: Uint8Array): Promise<string> {
  const encrypted = await platformCrypto.encrypt(plaintext);
  return base64url.encode(Buffer.from(encrypted));
}

/**
 * Decrypt ciphertext (string) into a Uint8Array plaintext
 * @param cipher
 * @private
 */
async function decrypt(cipher: string): Promise<Uint8Array> {
  const decoded = base64url.toBuffer(cipher);
  const plaintext = await platformCrypto.decrypt(decoded);
  return new Uint8Array(plaintext);
}

export namespace WalletStore {
  /**
   * Stores wallet seed & associated mnemonic
   * @param mnemonic the 12-word mnemonic to be saved
   * @param seed the unencrypted seed
   * @returns a promise which completes after storage
   */
  export async function store(mnemonic: string, seed: Uint8Array) {
    const binaryMnemonic: Uint8Array = new TextEncoder().encode(mnemonic);

    await getStore().dispatch(async (dispatch: AsyncDispatch): Promise<void> => {
      const encryptedSeed = await encrypt(seed);
      const encryptedMnemonic = await encrypt(binaryMnemonic);
      await dispatch(
        setWallet({
          seed: encryptedSeed,
          mnemonic: encryptedMnemonic,
        }),
      );
    });
  }

  /**
   * Retrieve the wallet seed mnemonic from the store
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
   * Retrieve the wallet seed from the store
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
   * Indicates whether a seed is present in the store
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
