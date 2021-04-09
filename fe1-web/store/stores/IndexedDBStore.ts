import { get, set, update } from 'idb-keyval';
import STRINGS from 'res/strings';

export interface WalletCryptoKey {
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
  private readonly publicKeyId: string = STRINGS.walletPrivateKeyId;

  private readonly privateKeyId: string = STRINGS.walletPublicKeyId;

  /**
   * adds an encryption/decryption key to the storage
   * @param key the key that will be used to encrypt/decrypt all tokens in the wallet
   */
  public async putKey(key : WalletCryptoKey): Promise<void> {
    await this.putPublicKey(key.publicKey);
    await this.putPrivateKey(key.privateKey);
  }

  private async putPublicKey(key : CryptoKey): Promise<void> {
    await set(this.publicKeyId, key);
  }

  private async putPrivateKey(key : CryptoKey): Promise<void> {
    await set(this.privateKeyId, key);
  }

  /**
   * returns the requested encryption key from the storage
   * @param type 'public' if the desired key is the public key,
   * 'private' if the desired key is the private key
   */
  public async getKey(type: string): Promise<CryptoKey> {
    if (type === STRINGS.walletPrivateKey) {
      const key = await this.getPrivateKey();
      return key;
    }
    const key = await this.getPublicKey();
    return key;
  }

  private async getPublicKey(): Promise<CryptoKey> {
    const pubKey = await get(this.publicKeyId);
    return pubKey;
  }

  private async getPrivateKey(): Promise<CryptoKey> {
    const secKey = await get(this.privateKeyId);
    return secKey;
  }

  /**
   * this is used to update the encryption key in case it will
   * every be necessary to change it
   * @param key the new key for encryption/decryption
   */
  public async updateKey(key : WalletCryptoKey): Promise<void> {
    await update(this.publicKeyId, () => key.publicKey);
    await update(this.privateKeyId, () => key.privateKey);
  }
}
