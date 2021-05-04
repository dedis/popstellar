/**
 * @return the SubtleCrypto library on browser for browser context
 */
export class WalletCryptoLibrary {
  public static getCrypto() : Crypto {
    return window.crypto;
  }
}
