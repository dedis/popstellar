import { WalletStore } from 'store/stores/WalletStore';
import * as bip39 from 'bip39';
import { WalletCryptographyHandler } from './WalletCryptographyHandler';

/**
 * bip39 library used for seed generation and verification
 * https://www.npmjs.com/package/bip39
 */

export interface WalletState {
  seed: ArrayBuffer;
}

/**
 * This class is responsible for all functions regarding
 * the hierarchical deterministic wallet.
 * The seed is created and verified here.
 * The tokens are created and retrieved here.
 */
export class HDWallet {
  /* The following constants are used (and common) in all derivation paths
   * example path: m / 888 / 0' / LAO_id' / roll_call_id */
  private static readonly PREFIX: string = 'm';

  private static readonly PURPOSE: string = '888';

  private static readonly ACCOUNT: string = '0';

  private static readonly SEPARATOR: string = '/';

  /* cryptography manager: encrypts the seed with RSA key stored in IndexedDB */
  private cryptoManager!: WalletCryptographyHandler;

  /* local copy of encrypted seed */
  private encryptedSeed!: ArrayBuffer;

  constructor(encryptedSeed: undefined | ArrayBuffer) {
    if (encryptedSeed !== undefined) {
      this.encryptedSeed = encryptedSeed;
    }
  }

  /**
   * this method initializes the wallet:
   * 1. creating the crypto-manager (RSA key in IndexedDB).
   * 2. encrypting the seed.
   * 3. storing it in the application state through reducer.
   * @param mnemonic the 12-word representation of the wallet seed
   * @return true if the given mnemonic is valid, false otherwise
   */
  public async initialize(mnemonic: string): Promise<boolean> {
    if (mnemonic === undefined) {
      console.error('Error while initializing the wallet seed: seed is undefined');
      return false;
    }

    const seedIsValid: boolean = bip39.validateMnemonic(mnemonic);
    if (!seedIsValid) {
      return false;
    }

    this.cryptoManager = new WalletCryptographyHandler();
    const seed: Uint8Array = await bip39.mnemonicToSeed(mnemonic);
    await this.cryptoManager.initWalletStorage();
    const encryptedSeed = await this.encryptSeedToStoreInState(seed);
    this.encryptedSeed = encryptedSeed;
    WalletStore.store(encryptedSeed);
    return true;
  }

  /**
   * retrieves and decrypts the wallet state (seed) from the application store.
   * @return the plaintext wallet seed as a Uint8Array
   */
  public async getDecryptedSeed(): Promise<Uint8Array> {
    const storedSeed = await WalletStore.get();
    const plaintext: ArrayBuffer = await this.cryptoManager
      .decrypt(storedSeed);
    const seed: Uint8Array = new Uint8Array(plaintext);
    return seed;
  }

  /**
   * @return the locally stored encrypted seed as a Uint8Array
   */
  public getEncryptedSeedToUint8Array(): Uint8Array {
    return new Uint8Array(this.encryptedSeed);
  }

  /**
   * uses npm bip39: https://www.npmjs.com/package/bip39
   * @return a new generated 12-word mnemonic which maps to a seed
   */
  public static getNewGeneratedMnemonicSeed() {
    return bip39.generateMnemonic();
  }

  /**
   * encrypts the numerical seed with wallet cryptography handler
   * @param seed the plaintext seed as Uint8Array
   * @private
   * @return the RSA-encrypted seed
   */
  private async encryptSeedToStoreInState(seed: Uint8Array): Promise<ArrayBuffer> {
    const encryptedSeed = await this.cryptoManager.encrypt(seed);
    return encryptedSeed;
  }

  /**
   * creates a new wallet object from the state (encryptedSeed)
   * @param encryptedSeed wallet's encrypted seed
   * @return a new wallet object initialized from the given encrypted seed
   */
  public async fromState(encryptedSeed: ArrayBuffer): Promise<HDWallet> {
    await this.cryptoManager.initWalletStorage();
    const walletFromState = new HDWallet(encryptedSeed);
    return walletFromState;
  }

  /**
   * returns the current wallet object to state (encryptedSeed)
   */
  public toState(): ArrayBuffer {
    return this.encryptedSeed;
  }
}
