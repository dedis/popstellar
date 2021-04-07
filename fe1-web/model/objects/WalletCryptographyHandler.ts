import { IndexedDBStore } from '../../store/stores/IndexedDBStore';
import STRINGS from '../../res/strings';

/**
 * @author Carlo Maria Musso
 * This class has the job of handling the cryptography functions of the wallet.
 * It interacts with the IndexedDB database in order to store and retrieve the
 * secret key. It also encrypts and decrypts the tokens with the retrieved key.
 */
export class WalletCryptographyHandler {
  private static readonly storageId: string = STRINGS.walletStorageName;

  /* there is one unique encryption/decryption key */
  private static readonly encryptionKeyId: number = 1;

  /* encryption/decryption algorithm (RSA) */
  private static readonly algorithm = {
    name: 'RSA-OAEP',
    modulusLength: 4096,
    publicExponent: new Uint8Array([1, 0, 1]),
    hash: 'SHA-256',
  };

  private static readonly keyUsages: KeyUsage[] = ['encrypt', 'decrypt'];

  /**
   * this method creates the storage entry in the key database
   * and then adds an RSA encryption/decryption key to it.
   */
  public static async initWalletStorage() {
    await IndexedDBStore.createObjectStore(this.storageId);
    await WalletCryptographyHandler.addEncryptionKey();
  }

  /**
   * this method closes the storage database
   */
  public static async closeWalletStorage() {
    IndexedDBStore.closeDatabase();
  }

  /**
   * encrypts the given ed25519 token with the RSA key stored in the indexedDB database
   * @param token ed25519 key toUint8Array()
   */
  public static async encrypt(token: Uint8Array) {
    const keyPair = (await WalletCryptographyHandler.getEncryptionKeyFromDatabase()).key;
    const cypheredToken = await window.crypto.subtle.encrypt(
      WalletCryptographyHandler.algorithm, keyPair.publicKey, token,
    );
    return cypheredToken;
  }

  /**
   * decrypts the encrypted ed25519 token with the RSA key stored in the indexedDB database
   * @param encryptedToken ed25519 encrypted token (ArrayBuffer)
   */
  public static async decrypt(encryptedToken: ArrayBuffer) {
    const keyPair = (await WalletCryptographyHandler.getEncryptionKeyFromDatabase()).key;
    const plaintextToken = await window.crypto.subtle.decrypt(
      WalletCryptographyHandler.algorithm, keyPair.privateKey, encryptedToken,
    );
    return plaintextToken;
  }

  /**
   * Adds an RSA encryption/decryption key,
   * interacts with IndexedDB browser database.
   */
  private static async addEncryptionKey() {
    const key = {
      id: this.encryptionKeyId,
      key: await this.generateRSAKey(),
    };
    await IndexedDBStore.putEncryptionKey(this.storageId, key);
  }

  /**
   * This function retrieves the RSA key from database
   */
  private static getEncryptionKeyFromDatabase = () => IndexedDBStore.getEncryptionKey(
    WalletCryptographyHandler.storageId,
    WalletCryptographyHandler.encryptionKeyId,
  );

  /**
   * generates the RSA key according to specified algorithm
   */
  private static generateRSAKey = async () => {
    const keyPair = await window.crypto.subtle.generateKey(
      WalletCryptographyHandler.algorithm, false, WalletCryptographyHandler.keyUsages,
    );
    return {
      publicKey: keyPair.publicKey,
      privateKey: keyPair.privateKey,
    };
  };
}
