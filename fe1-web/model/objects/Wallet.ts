import { sign } from 'tweetnacl';
import { encodeBase64 } from 'tweetnacl-util';
import { IndexedDBStore } from '../../store/stores/IndexedDBStore';
import STRINGS from '../../res/strings';

/**
 * @author Carlo Maria Musso
 *
 *
 */
export class Wallet {
  private readonly storageId: string;

  private readonly cryptKeyDatabase: IndexedDBStore;

  /**
   * creates a wallet object
   */
  constructor() {
    this.cryptKeyDatabase = new IndexedDBStore(STRINGS.walletDatabaseName);
    this.storageId = STRINGS.walletStorageName;
    this.initWalletStorage()
      .then((retVal) => console.log(`storage construction return value is ${retVal}`));
  }

  /**
   * this method creates the storage entry in the database
   */
  public async initWalletStorage() {
    await this.cryptKeyDatabase
      .createObjectStore(this.storageId);
  }

  /**
   * Adds an encryption/decryption key (random for the moment),
   * interacts with IndexedDB browser database.
   */
  public async addEncryptionKey() {
    /* Here the real encryption/decryption key which will be used
      is still missing for the moment it is generated randomly */
    const key :string = Wallet
      .generateKeyPairEntry().privateKey.toString();

    this.cryptKeyDatabase
      .putEncryptionKey(this.storageId, key)
      .then((retVal) => console.log(`addEncryptionKey return value is ${retVal}`));
  }

  /**
   * deletes the encrypted key associated requested in argument id,
   * interacts with IndexedDB browser database.
   * @id the number of the key in storage (technically there is only one key)
   */
  public async deleteEncryptionKey(id: number) {
    if (id === null) {
      throw new Error('Error encountered while deleting encryption/decryption key : null argument');
    }
    this.cryptKeyDatabase
      .deleteEncryptionKey(this.storageId, id)
      .then((retVal) => console.log(`deleteEncryptionKey return value is ${retVal}`));
  }

  /**
   * generates a new public and private key
   *
   * This method will be deleted once the crypto web API part
   * is implemented having the right encryption key.
   */
  private static generateKeyPairEntry() {
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
