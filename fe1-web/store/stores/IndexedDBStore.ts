import { get, set, update } from 'idb-keyval';
import STRINGS from 'res/strings';

export interface EncryptionKey {
  privateKey: CryptoKey,
  publicKey: CryptoKey,
}

/**
 * This class represents the browser storage used to securely store the (RSA) encryption key
 * used to encrypt and decrypt tokens stored in the react state.
 * Implemented using IndexedDB database on web browser in a secure way following the below link:
 * https://blog.engelke.com/2014/09/19/saving-cryptographic-keys-in-the-browser/
 */
export class IndexedDBStore {
  private readonly publicKeyId: string = STRINGS.walletEncryptionPublicKeyId;

  private readonly privateKeyId: string = STRINGS.walletEncryptionPrivateKeyId;

  /**
   * adds an encryption/decryption key to the storage
   * @param key the key that will be used to encrypt/decrypt all tokens in the wallet
   */
  public async putEncryptionKey(key : EncryptionKey): Promise<void> {
    await this.putPublicEncryptionKey(key.publicKey);
    await this.putPrivateEncryptionKey(key.privateKey);
  }

  private async putPublicEncryptionKey(key : CryptoKey): Promise<void> {
    await set(this.publicKeyId, key);
  }

  private async putPrivateEncryptionKey(key : CryptoKey): Promise<void> {
    await set(this.privateKeyId, key);
  }

  /**
   * returns the requested encryption key from the storage
   * @param type 'public' if the desired key is the public key,
   * 'private' if the desired key is the private key
   */
  public async getEncryptionKey(type: string): Promise<CryptoKey> {
    if (type === STRINGS.walletPrivateKey) {
      const key = await this.getPrivateEncryptionKey();
      return key;
    }
    const key = await this.getPublicEncryptionKey();
    return key;
  }

  private async getPublicEncryptionKey(): Promise<CryptoKey> {
    const pubKey = await get(this.publicKeyId);
    return pubKey;
  }

  private async getPrivateEncryptionKey(): Promise<CryptoKey> {
    const secKey = await get(this.privateKeyId);
    return secKey;
  }

  /**
   * this is used to update the encryption key in case it will
   * every be necessary to change it
   * @param key the new key for encryption/decryption
   */
  public async updateEncryptionKey(key : EncryptionKey): Promise<void> {
    await update(this.publicKeyId, () => key.publicKey);
    await update(this.privateKeyId, () => key.privateKey);
  }
}
