import { IDBPDatabase, openDB } from 'idb';

/**
 * @author Carlo Maria Musso
 * This class represents the browser storage used to securely store the encryption key
 * used to encrypt and decrypt tokens stored in the react state.
 * Implemented using IndexedDB database on web browser in a secure way following the below link:
 * https://blog.engelke.com/2014/09/19/saving-cryptographic-keys-in-the-browser/
 *
 * note - the Web Cryptography part is not implemented, for the moment the encryption key
 * is simply randomly generated in this class.
 */
export class IndexedDBStore {
  private readonly database: string;

  private db: any;

  /**
   * @param database the name of the database
   */
  constructor(database: string) {
    if (database === null) {
      throw new Error('Error encountered while creating storage : null database name');
    }
    this.database = database;
  }

  /**
   * This method creates an object storage in the database which will contain
   * the encryption/decryption key for all tokens in the wallet.
   * @param storageId the id of the secure storage
   */
  public async createObjectStore(storageId: string) {
    try {
      this.db = await openDB(this.database, 2, {
        upgrade(db: IDBPDatabase) {
          if (!db.objectStoreNames.contains(storageId)) {
            db.createObjectStore(storageId, {
              autoIncrement: true,
              keyPath: 'id',
            });
          }
        },
      });
    } catch (error) {
      throw new Error(`Error encountered: ${error}`);
    }
    return true;
  }

  /**
   * adds an encryption/decryption key to the storage
   * @param storageId the id of the secure storage
   * @param encryptionKey the key that will be used to encrypt/decrypt all tokens in the wallet
   */
  public async putEncryptionKey(storageId: string, encryptionKey: { privateKey: string }) {
    if (storageId === null || encryptionKey === null) {
      throw new Error('Error encountered while adding encrypt/decrypt key to storage : null parameters');
    }

    const tx = this.db.transaction(storageId, 'readwrite');
    const store = tx.objectStore(storageId);
    const result = await store.put(encryptionKey);
    console.log('Put Data ', JSON.stringify(result));
    return result;
  }

  /**
   * returns the requested encryption key from the storage
   * @param storageId the id of the secure storage
   * @param id the number of the key in storage (technically there is only one key)
   */
  public async getEncryptionKey(storageId: string, id: number) {
    if (storageId === null || id === null) {
      throw new Error('Error encountered while retrieving encrypt/decrypt key from storage : null parameters');
    }
    const tx = this.db.transaction(storageId, 'readonly');
    const store = tx.objectStore(storageId);
    const result = await store.get(id);
    console.log('Get Data ', JSON.stringify(result));
    return result;
  }

  /**
   * removes the requested encryption key from the secure storage
   * @param storageId the id of the secure storage
   * @param id the number of the key in storage (technically there is only one key)
   */
  public async deleteEncryptionKey(storageId: string, id: number) {
    if (storageId === null || id === null) {
      throw new Error('Error encountered while deleting encrypt/decrypt key from storage : null parameters');
    }

    const tx = this.db.transaction(storageId, 'readwrite');
    const store = tx.objectStore(storageId);
    const result = await store.get(id);
    if (!result) {
      console.log('token not found', id);
      return result;
    }
    await store.delete(id);
    console.log('Deleted Data', id);
    return id;
  }
}
