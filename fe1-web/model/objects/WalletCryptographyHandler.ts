import NodeRSA from 'node-rsa';
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

  private static readonly encryptionKeyId: number = 1;

  private static nodeRSA: NodeRSA;

  /**
   * this method creates the storage entry in the database
   */
  public static async initWalletStorage() {
    await IndexedDBStore.createObjectStore(this.storageId);
    await WalletCryptographyHandler.addEncryptionKey();
    const key = await IndexedDBStore.getEncryptionKey(WalletCryptographyHandler.storageId,
      WalletCryptographyHandler.encryptionKeyId);
    WalletCryptographyHandler.nodeRSA = new NodeRSA(key);
  }

  /**
   * encrypts the given ed25519 token with the RSA key stored in the indexedDB database
   * @param token ed25519 key
   */
  public static encrypt = async (token: string) => WalletCryptographyHandler.nodeRSA.encrypt(token, 'base64');

  /**
   * decrypts the encrypted ed25519 token with the RSA key stored in the indexedDB database
   * @param encryptedToken ed25519 encrypted token
   */
  public static decrypt = async (encryptedToken: string) => WalletCryptographyHandler.nodeRSA.decrypt(encryptedToken, 'utf8');

  /**
   * Adds an RSA encryption/decryption key,
   * interacts with IndexedDB browser database.
   */
  private static async addEncryptionKey() {
    const key: { id: number, key: string } = {
      id: this.encryptionKeyId,
      key: this.generateRSAKey(),
    };

    await IndexedDBStore.putEncryptionKey(this.storageId, key);
  }

  /**
   * generates the RSA key
   */
  private static generateRSAKey = () => new NodeRSA({ b: 512 }).exportKey();
}
