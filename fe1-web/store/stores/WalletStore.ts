import { IDBPDatabase, openDB } from 'idb';
import { sign } from 'tweetnacl';
import { encodeBase64 } from 'tweetnacl-util';

/**
 * @author Carlo Maria Musso
 * This class represents the wallet storage: securely stores the keypairs added to the wallet.
 * Implemented using IndexedDB database on web browser in a secure way following the below link:
 * https://blog.engelke.com/2014/09/19/saving-cryptographic-keys-in-the-browser/
 *
 * note - the Web Cryptography part is not implemented, for the moment keys are stored in plaintext
 */
export class WalletStore {
  private readonly database: string;

  private db: any;

  /**
   * @param database the name of the database
   */
  constructor(database: string) {
    if (database === null) {
      throw new Error('Error encountered while creating wallet storage : null database name');
    }
    this.database = database;
  }

  /**
   * This method creates an object store in the storage which will contain
   * entries (e.g. the various KeyPairs per wallet). This object is identified
   * by the walletId, there is one object store per wallet in the database.
   * @param walletId the id of the wallet
   */
  public async createObjectStore(walletId: string) {
    try {
      this.db = await openDB(this.database, 2, {
        upgrade(db: IDBPDatabase) {
          if (!db.objectStoreNames.contains(walletId)) {
            db.createObjectStore(walletId, { keyPath: 'id' });
          }
        },
      });
    } catch (error) {
      throw new Error(`Error encountered: ${error}`);
    }
    return true;
  }

  /**
   * adds a key/value pair to the wallet object store in database
   * @param walletId the id of the wallet to which add the key
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  public async putToken(walletId: string, laoId: string, rollCallId: string) {
    if (walletId === null || laoId === null || rollCallId === null) {
      throw new Error('Error encountered while adding roll call token to Wallet : null parameters');
    }
    const keyPair = WalletStore.generateKeyPairEntry(WalletStore
      .buildTokenIdentifier(laoId, rollCallId));

    const tx = this.db.transaction(walletId, 'readwrite');
    const store = tx.objectStore(walletId);
    const result = await store.put(keyPair);
    console.log('Put Data ', JSON.stringify(result));
    return result;
  }

  /**
   * returns the keyPair (token) for the given laoId-rollCallId key from the wallet
   * @param walletId the id of the wallet to which contains the token
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  public async getToken(walletId: string, laoId: string, rollCallId: string) {
    if (walletId === null || laoId === null || rollCallId === null) {
      throw new Error('Error encountered while retrieving roll call token from Wallet : null parameters');
    }
    const tx = this.db.transaction(walletId, 'readonly');
    const store = tx.objectStore(walletId);
    const result = await store.get(WalletStore.buildTokenIdentifier(laoId, rollCallId));
    console.log('Get Data ', JSON.stringify(result));
    return result;
  }

  /**
   * removes the requested token from the wallet
   * @param walletId the id of the wallet to which contains the token
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  public async deleteToken(walletId: string, laoId: string, rollCallId: string) {
    if (walletId === null || laoId === null || rollCallId === null) {
      throw new Error('Error encountered while deleting roll call token from Wallet : null parameters');
    }
    const laoAndRcId = WalletStore.buildTokenIdentifier(laoId, rollCallId);
    const tx = this.db.transaction(walletId, 'readwrite');
    const store = tx.objectStore(walletId);
    const result = await store.get(laoAndRcId);
    if (!result) {
      console.log('token not found', laoAndRcId);
      return result;
    }
    await store.delete(laoAndRcId);
    console.log('Deleted Data', laoAndRcId);
    return laoAndRcId;
  }

  /**
   * generates a new public and private key
   */
  private static generateKeyPairEntry(id: string) {
    const pair = sign.keyPair();
    const keys = {
      pubKey: encodeBase64(pair.publicKey),
      secKey: encodeBase64(pair.secretKey),
    };
    return {
      id: id,
      publicKey: keys.pubKey,
      privateKey: keys.secKey,
    };
  }

  private static buildTokenIdentifier(laoId: string, rollCallId: string): string {
    return laoId.toString().concat(rollCallId.toString());
  }
}
