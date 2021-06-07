import { WalletStore } from 'store/stores/WalletStore';
import * as bip39 from 'bip39';
import { derivePath, getPublicKey } from 'ed25519-hd-key';
import { WalletCryptographyHandler } from './WalletCryptographyHandler';
import { Hash } from './Hash';
import { KeyPair } from './KeyPair';
import { PublicKey } from './PublicKey';
import { PrivateKey } from './PrivateKey';

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

    const seedIsValid: boolean = bip39.validateMnemonic(mnemonic.toLowerCase());
    if (!seedIsValid) {
      return false;
    }

    this.cryptoManager = new WalletCryptographyHandler();
    await this.cryptoManager.initWalletStorage();
    const seed: Uint8Array = await bip39.mnemonicToSeed(mnemonic.toLowerCase());
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

  /**
   * This is the main function for the wallet to find all the tokens associated with it, by checking
   * through all known Laos and Roll Calls of that LAO weather or not the token generated bu the
   * respective LAO and RollCall Id is actually present in the roll call history list (namely the
   * public key was scanned and added to server during the roll call.
   *
   * @param allKnownLaoRollCalls This is the map the wallet should recieve mapping all LAO and Roll
   * Call ids to a list of public keys which where present during the roll call.
   */
  public async recoverAllKeys(allKnownLaoRollCalls: Map<[Hash, Hash], string[]>):
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
      const publicKey: string = keyPair.publicKey.toString();

      if (attendees.indexOf(publicKey) !== -1) {
        cachedKeyPairs.set([laoId, rollCallId], publicKey);
      }
    });
  }

  /**
   * Returns the token created by the path and wallet seed according to LAOId and RollCallId
   * @param laoId the id of the LAO
   * @param rollCallId the id of the Roll Call
   */
  public async generateToken(laoId: Hash, rollCallId:Hash): Promise<KeyPair> {
    return this.generateKeyPair(laoId, rollCallId)
      .then((keyPair) => keyPair);
  }

  public tokenIsInRollCall(laoId: Hash, rollCallId: Hash): boolean {
    // TODO
    return true;
  }

  private async generateKeyPair(laoId: Hash, rollCallId:Hash): Promise<KeyPair> {
    const path: string = HDWallet.PREFIX
      .concat(HDWallet.PATH_SEPARATOR
        .concat(HDWallet.PURPOSE
          .concat(HDWallet.HARDENED_SYMBOL
            .concat(HDWallet.PATH_SEPARATOR
              .concat(HDWallet.ACCOUNT
                .concat(HDWallet.HARDENED_SYMBOL)
                .concat(HDWallet.idToPath(laoId))
                .concat((HDWallet.idToPath(rollCallId))))))));

    return this.generateKeyFromPath(path);
  }

  private async generateKeyFromPath(path: string): Promise<KeyPair> {
    return this.getDecryptedSeed()
      .then((seedArray) => {
        const hexSeed = Buffer.from(seedArray)
          .toString('hex');

        const { key } = derivePath(path, hexSeed);
        const pubKey = getPublicKey(key, false);

        return new KeyPair({
          publicKey: new PublicKey(pubKey.toString('hex')),
          privateKey: new PrivateKey(key.toString('hex')),
        });
      });
  }

  private static idToPath(idIn: Hash): string {
    const id = idIn.toBuffer();
    let idToPath: string = '';

    const remainder = id.length % 3;

    let i;
    for (i = 0; i + 3 <= id.length; i += 3) {
      idToPath = idToPath.concat(HDWallet.PATH_SEPARATOR
        .concat(String(id.readUInt8(i)))
        .concat(String(id.readUInt8(i + 1)))
        .concat(String(id.readUInt8(i + 2)))
        .concat(HDWallet.HARDENED_SYMBOL));
    }

    if (remainder === 1) {
      idToPath = idToPath.concat(HDWallet.PATH_SEPARATOR
        .concat(String(id.readUInt8(i)))
        .concat(HDWallet.HARDENED_SYMBOL));
    } else if (remainder === 2) {
      idToPath = idToPath.concat(HDWallet.PATH_SEPARATOR
        .concat(String(id.readUInt8(i)))
        .concat(String(id.readUInt8(i + 1)))
        .concat(HDWallet.HARDENED_SYMBOL));
    }
    return idToPath;
  }
}
