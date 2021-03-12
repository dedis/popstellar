import { encodeBase64 } from 'tweetnacl-util';
import { sign } from 'tweetnacl';
import { SimpleWalletStore } from 'store/stores/SimpleWalletStore';
import { Hash } from './Hash';
import { Base64Data } from './Base64';
import { KeyPair, KeyPairState } from './KeyPair';

/**
 * This class represents a simple wallet object (in the sense that  it is
 * not a HD wallet but a basic impelentation). This wallet generates a
 * public and private key for each roll call attended in a LAO. The wallet
 * then relies on a separate wallet storage (but associated with a walletId
 * to securely store and retrieve the keypairs.
 *
 * note - all the add/get/remove methods for the database throw exceptions if
 * the operation is not possible, should they instead return true/false?
 *
 * note - there is no way to remove a keyPair from a wallet thorugh the wallet
 * object (even though it could be useful) since it implies a more developed
 * reasoning in terms of security.
 */
export class SimpleWalletObject {
  private walletId: Hash;

  private subscribedLaos: Set<Hash> = new Set<Hash>();

  private laoToRollCallsMap: Map<Hash, Set<Hash>> = new Map<Hash, Set<Hash>>();

  private storage: SimpleWalletStore;

  constructor(walletId: Hash) {
    if (walletId === null) {
      throw new Error('Error encountered while creating a wallet object : undefined/null wallet Id');
    }

    this.walletId = walletId;
    this.storage = new SimpleWalletStore(walletId);
  }

  /**
   * Add a local autonomous organisation to the wallet
   * @param laoId the id for the LAO
   */
  public addLao(laoId: Hash) {
    if (laoId === null) {
      throw new Error('Error encountered while adding LAO : null LAO ID');
    }
    if (this.subscribedLaos.has(laoId)) {
      throw new Error('Error encountered while adding LAO : this LAOId already exists');
    }
    this.subscribedLaos.add(laoId);
    const emptySet: Set<Hash> = new Set<Hash>();
    this.laoToRollCallsMap.set(laoId, emptySet);
  }

  /**
   * Adds the id of an attended roll call to a specific lao id entry.
   * Also generates public and secret key pair to store in wallet via
   * a private method.
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  public addTokenForRollCallAttendance(laoId: Hash, rollCallId: Hash) {
    if (laoId === null || rollCallId == null) {
      throw new Error('Error encountered while adding roll call to LAO : null argument');
    }

    if (!this.subscribedLaos.has(laoId)) {
      this.addLao(laoId);
    }

    const updatedSet: Set<Hash> = this.laoToRollCallsMap.get(laoId)!;
    if (updatedSet.has(rollCallId)) {
      throw new Error('Error encountered while adding roll call to wallet : this rollCallId already has an associated keypair');
    }
    updatedSet.add(rollCallId);
    this.laoToRollCallsMap.set(laoId, updatedSet);
    this.generateKeyPairAndAddToWallet(laoId, rollCallId);
  }

  /**
   * returns the keypair associated with the attendance of the roll
   * call organised by the LAO passed in parameter.
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  public findKeyPair(laoId: Hash, rollCallId: Hash) {
    if (laoId === null || rollCallId == null) {
      throw new Error('Error encountered while finding Key Pair -> null argument');
    }
    if (
      !(
        this.subscribedLaos.has(laoId)
        && this.laoToRollCallsMap.get(laoId)!.has(rollCallId)
      )
    ) {
      throw new Error('Error encountered while retrieving keyPair : the LAO or roll call ID was never added to the wallet');
    }
    const key: string = SimpleWalletObject.buildStorageKey(laoId, rollCallId);
    return this.storage.getKeyPairFromWallet(key);
  }

  /**
   * Builds the key value, creates the KeyPair from state and adds it
   * to the wallet storage.
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  private generateKeyPairAndAddToWallet(laoId: Hash, rollCallId: Hash) {
    const key: string = SimpleWalletObject.buildStorageKey(laoId, rollCallId);
    const keyPair: KeyPair = KeyPair.fromState(SimpleWalletObject.generateKeyPair());

    /* no check needed since done in the caller method */
    this.storage.addKeyPairToWallet(key, keyPair);
  }

  /**
   * generates the key to the storage which is the concatenation
   * of the lao ID and the roll call ID
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  private static buildStorageKey(laoId: Base64Data, rollCallId: Base64Data) {
    const key: string = laoId.toString().concat(rollCallId.toString());
    return key;
  }

  /**
   * generates a new public and private key
   */
  private static generateKeyPair(): KeyPairState {
    const pair = sign.keyPair();
    const keys = {
      pubKey: encodeBase64(pair.publicKey),
      secKey: encodeBase64(pair.secretKey),
    };
    return {
      publicKey: keys.pubKey,
      privateKey: keys.secKey,
    };
  }
}
