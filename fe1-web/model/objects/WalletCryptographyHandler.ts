import { sign } from 'tweetnacl';
import { encodeBase64 } from 'tweetnacl-util';
import { IndexedDBStore } from '../../store/stores/IndexedDBStore';
import STRINGS from '../../res/strings';

/**
 * @author Carlo Maria Musso
 * This class has the job of handling the cryptography functions of the wallet.
 * It interacts with the IndexedDB database in order to store and retrieve the
 * secret key. It will also encrypt and decrypt the tokens with the retrieved
 * key and then return it to the wallet object.
 */
export class WalletCryptographyHandler {
  private readonly storageId: string;

  private readonly cryptKeyDatabase: IndexedDBStore;

  private numberOfEncryptionKeys: number;

  /**
   * creates the wallet cryptography handler
   */
  constructor() {
    this.cryptKeyDatabase = new IndexedDBStore(STRINGS.walletDatabaseName);
    this.storageId = STRINGS.walletStorageName;
    this.numberOfEncryptionKeys = 0;
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
    if (this.numberOfEncryptionKeys > 0) {
      console.log('Error encountered while adding encryption/decryption key : an encryption key is already in db');
      return;
    }
    /* Here the real encryption/decryption key which will be used
      is still missing for the moment it is generated randomly */
    const key: { privateKey: string } = {
      privateKey: WalletCryptographyHandler
        .generateKeyPairEntry().privateKey,
    };

    this.cryptKeyDatabase
      .putEncryptionKey(this.storageId, key)
      .then((retVal) => {
        this.numberOfEncryptionKeys += 1;
        console.log(`addEncryptionKey return value is ${retVal}`);
      });
  }

  /**
   * deletes the encrypted key associated requested in argument id,
   * interacts with IndexedDB browser database.
   * @id the number of the key in storage (there is only one key)
   */
  public async deleteEncryptionKey(id: number) {
    if (this.numberOfEncryptionKeys <= 0) {
      console.log('Error encountered while deleting encryption/decryption key : no encryption key in db');
      return;
    }
    if (id === null) {
      throw new Error('Error encountered while deleting encryption/decryption key : null argument');
    }
    this.cryptKeyDatabase
      .deleteEncryptionKey(this.storageId, id)
      .then((retVal) => {
        this.numberOfEncryptionKeys -= 1;
        console.log(`deleteEncryptionKey return value is ${retVal}`);
      });
  }

  /**
   * generates a new public and private key
   *
   * This method will be deleted once the crypto web API
   * part is implemented having the right encryption key.
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
