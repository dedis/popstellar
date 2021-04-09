import { IndexedDBStore, EncryptionKey } from 'store/stores/IndexedDBStore';

/**
 * This class has the job of handling the cryptography functions of the wallet.
 * It interacts with the IndexedDB database in order to store and retrieve the
 * secret key. It also encrypts and decrypts the tokens with the retrieved key.
 */
export class WalletCryptographyHandler {
  /* encryption/decryption algorithm (RSA) */
  private readonly algorithm = {
    name: 'RSA-OAEP',
    modulusLength: 4096,
    publicExponent: new Uint8Array([1, 0, 1]),
    hash: 'SHA-256',
  };

  private readonly keyUsages: KeyUsage[] = ['encrypt', 'decrypt'];

  private keyDatabase: IndexedDBStore = new IndexedDBStore();

  /**
   * this method adds an RSA encryption/decryption key,
   * interacts with IndexedDB browser database.
   */
  public async initWalletStorage(): Promise <void> {
    const key: EncryptionKey = await this.generateRSAKey();
    await this.keyDatabase.putEncryptionKey(key);
  }

  /**
   * encrypts the given ed25519 token with the RSA key stored in the indexedDB database
   * @param token ed25519 key toUint8Array()
   */
  public async encrypt(token: Uint8Array): Promise<ArrayBuffer> {
    const key = await this.getKeyFromDatabase('public');
    const cypheredToken = await window.crypto.subtle.encrypt(this.algorithm, key, token);
    return cypheredToken;
  }

  /**
   * decrypts the encrypted ed25519 token with the RSA key stored in the indexedDB database
   * @param encryptedToken ed25519 encrypted token (ArrayBuffer)
   */
  public async decrypt(encryptedToken: ArrayBuffer): Promise<ArrayBuffer> {
    const key = await this.getKeyFromDatabase('private');
    const plaintextToken = await window.crypto.subtle.decrypt(this.algorithm, key, encryptedToken);
    return plaintextToken;
  }

  /**
   * This function retrieves the RSA key from database
   * @param type 'public' if the desired key is the public key,
   * 'private' if the desired key is the private key
   */
  private getKeyFromDatabase: (type: string) => Promise<CryptoKey> =
  async (type: string) => {
    const key: CryptoKey = await this.keyDatabase.getEncryptionKey(type);
    return key;
  };

  /**
   * generates the RSA key according to specified algorithm
   */
  private generateRSAKey: () => Promise<EncryptionKey> =
  async () => {
    const keyPair = await window.crypto.subtle.generateKey(
      this.algorithm, false, this.keyUsages,
    );
    return {
      publicKey: keyPair.publicKey,
      privateKey: keyPair.privateKey,
    };
  };
}
