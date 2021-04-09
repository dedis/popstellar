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
  private publicKeyId: string = STRINGS.walletEncryptionPublicKeyId;

  private privateKeyId: string = STRINGS.walletEncryptionPrivateKeyId;

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
   * @param id the number of the key in storage (technically there is only one key)
   */
  public async getEncryptionKey(type: string): Promise<CryptoKey> {
    if (type === 'private') {
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

  public async updateEncryptionKey(key : EncryptionKey): Promise<void> {
    await update(this.publicKeyId, () => key.publicKey);
    await update(this.privateKeyId, () => key.privateKey);
  }
}
