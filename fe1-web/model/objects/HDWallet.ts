import { WalletStore } from 'store/stores/WalletStore';
import * as bip39 from 'bip39';
import { derivePath, getPublicKey } from 'ed25519-hd-key';
import { WalletCryptographyHandler } from './WalletCryptographyHandler';
import { Hash } from './Hash';

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

  private static readonly PATH_SEPARATOR: string = '/';

  private static readonly HARDENED_SYMBOL: string = "'";

  /* cryptography manager: encrypts the seed with RSA key stored in IndexedDB */
  private cryptoManager!: WalletCryptographyHandler;

  /* local copy of encrypted seed */
  private encryptedSeed!: ArrayBuffer;

  /**
   * a wallet can be created empty and then initialized or
   * directly with a seed recovered from redux state
   * @param encryptedSeed the encrypted seed got from redux state
   */
  constructor(encryptedSeed?: ArrayBuffer) {
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
    await this.cryptoManager.initWalletStorage();
    const seed: Uint8Array = await bip39.mnemonicToSeed(mnemonic);
    this.encryptedSeed = await this.encryptSeedToStoreInState(seed);
    WalletStore.store(HDWallet.serializeEncryptedSeed(this.encryptedSeed));
    return true;
  }

  /**
   * retrieves and decrypts the wallet state (seed) from the application store.
   * @return the plaintext wallet seed as a Uint8Array
   */
  public async getDecryptedSeed(): Promise<Uint8Array> {
    const encodedStoredSeed: string = await WalletStore.get();
    const storedSeed = HDWallet.deserializeEncryptedSeed(encodedStoredSeed);
    const plaintext: ArrayBuffer = await this.cryptoManager
      .decrypt(storedSeed);
    return new Uint8Array(plaintext);
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
    return this.cryptoManager.encrypt(seed);
  }

  /**
   * creates a new wallet object from the state (encryptedSeed)
   * @param encryptedSerializedSeed wallet's encrypted seed recovered from state
   * @return a new wallet object initialized from the given encrypted seed
   */
  public static async fromState(encryptedSerializedSeed: string): Promise<HDWallet> {
    const wallet: HDWallet = new
    HDWallet(HDWallet.deserializeEncryptedSeed(encryptedSerializedSeed));

    wallet.cryptoManager = new WalletCryptographyHandler();
    await wallet.cryptoManager.initWalletStorage();
    return wallet;
  }

  /**
   * returns the current wallet object to state (encryptedSeed)
   * serialized as a String in order to be stored in redux state
   */
  public toState(): string {
    return HDWallet.serializeEncryptedSeed(this.encryptedSeed);
  }

  /**
   * Transforms the given encrypted seed (ArrayBuffer) in string,
   * only primitive types can be stored in redux state
   * @param encryptedSeed the encrypted seed as an ArrayBuffer
   * @private
   */
  private static serializeEncryptedSeed(encryptedSeed: ArrayBuffer): string {
    return new Uint8Array(encryptedSeed).toString();
  }

  /**
   * Transforms the serialized encrypted seed stored in state back to the
   * original ArrayBuffer encrypted seed used by the cryptography handler
   * @param encryptedSeedEncoded
   * @private
   */
  private static deserializeEncryptedSeed(encryptedSeedEncoded: string): ArrayBuffer {
    const buffer = encryptedSeedEncoded.split(',');
    const bufView = new Uint8Array(buffer.length);
    for (let i = 0; i < buffer.length; i += 1) {
      bufView[i] = Number(buffer[i]);
    }
    return bufView.buffer;
  }

  public async recoverTokens(): Promise<Map<[Hash, Hash], string>> {
    // ====================================================================================
    // garbage effort river orphan negative kind outside quit hat camera approve first
    const laoId1: Hash = Hash.fromString('LAO1');
    const laoId2: Hash = Hash.fromString('LAO2');

    const rollCallId1: Hash = Hash.fromString('LAO1RC1');
    const rollCallId2: Hash = Hash.fromString('LAO1RC2');
    const rollCallId3: Hash = Hash.fromString('LAO2RC1');
    const rollCallId4: Hash = Hash.fromString('LAO2RC2');

    const testMap: Map<[Hash, Hash], string[]> = new Map();

    testMap.set([laoId1, rollCallId1], ['001a8828bdb40320421f4769a8ca16f652ddaad5cd13213578eddc98698200c140', '']);
    testMap.set([laoId1, rollCallId2], ['fffffffffffffff', '', 'fffffffffffffff', '']);
    testMap.set([laoId2, rollCallId3], ['fffffffffffffff', '', '00cf3cb2d461cf51feeab3e8f0ffbcd2ebc8055ac001f858063fce4f1c0cc456a0']);
    testMap.set([laoId2, rollCallId4], ['0059d8a5fa53d26b816ba250d46ebbede4d93eda3531a89ff329698e5a2d9be753', 'fffffffffffffff']);

    // ====================================================================================
    return this.recoverAllKeys(testMap);
  }

  private async recoverAllKeys(allKnownLaoRollCalls: Map<[Hash, Hash], string[]>):
  Promise<Map<[Hash, Hash], string>> {
    if (allKnownLaoRollCalls === undefined) {
      throw Error('Error while recovering keys from wallet: undefined parameter');
    }

    const cachedKeyPairs: Map<[Hash, Hash], string> = new Map();

    allKnownLaoRollCalls.forEach((attendees: string[], laoAndRollCallId: [Hash, Hash]) => {
      const laoId: Hash = laoAndRollCallId[0];
      const rollCallId: Hash = laoAndRollCallId[1];
      this.recoverKey(laoId, rollCallId, attendees, cachedKeyPairs);
    });

    console.log(cachedKeyPairs);
    return cachedKeyPairs;
  }

  private async recoverKey(laoId: Hash, rollCallId: Hash, attendees: string[],
    cachedKeyPairs: Map<[Hash, Hash], string>) {
    this.generateKeyPair(laoId, rollCallId).then((keyPair) => {
      const publicKey: string = keyPair.publicKey.toString('hex');

      if (attendees.indexOf(publicKey) !== -1) {
        cachedKeyPairs.set([laoId, rollCallId], publicKey);
      }
    });
  }

  public async generateKeyPair(laoId: Hash, rollCallId:Hash):
  Promise<{ privateKey: Buffer, publicKey: Buffer }> {
    const path: string = HDWallet.PREFIX
      .concat(HDWallet.PATH_SEPARATOR
        .concat(HDWallet.PURPOSE
          .concat(HDWallet.HARDENED_SYMBOL
            .concat(HDWallet.PATH_SEPARATOR
              .concat(HDWallet.ACCOUNT
                .concat(HDWallet.HARDENED_SYMBOL)
                .concat(HDWallet.idToPath(laoId))
                .concat((HDWallet.idToPath(rollCallId))))))));

    console.log(path);
    return this.generateKeyFromPath(path);
  }

  private async generateKeyFromPath(path: string):
  Promise<{ privateKey: Buffer, publicKey: Buffer }> {
    return this.getDecryptedSeed()
      .then((seedArray) => {
        const hexSeed = Buffer.from(seedArray)
          .toString('hex');
        const { key } = derivePath(path, hexSeed);
        const publicKey = getPublicKey(key);

        console.log('===========================================================');
        console.log('private key');
        console.log(key.toString('hex'));
        console.log('public key');
        console.log(publicKey.toString('hex'));
        console.log('===========================================================');

        return {
          privateKey: key,
          publicKey: publicKey,
        };
      });
  }

  private static idToPath(id: Hash): string {
    let idToPath: string = '';
    const remainder = id.length % 3;

    let i;
    for (i = 0; i + 3 <= id.length; i += 3) {
      idToPath = idToPath.concat(HDWallet.PATH_SEPARATOR
        .concat(String(id.charCodeAt(i)))
        .concat(String(id.charCodeAt(i + 1)))
        .concat(String(id.charCodeAt(i + 2)))
        .concat(HDWallet.HARDENED_SYMBOL));
    }

    if (remainder === 1) {
      idToPath = idToPath.concat(HDWallet.PATH_SEPARATOR
        .concat(String(id.charCodeAt(i)))
        .concat(HDWallet.HARDENED_SYMBOL));
    } else if (remainder === 2) {
      idToPath = idToPath.concat(HDWallet.PATH_SEPARATOR
        .concat(String(id.charCodeAt(i)))
        .concat(String(id.charCodeAt(i + 1)))
        .concat(HDWallet.HARDENED_SYMBOL));
    }
    return idToPath;
  }
}
