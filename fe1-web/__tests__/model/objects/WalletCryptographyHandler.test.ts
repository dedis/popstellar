import 'jest-extended';
import { WalletCryptographyHandler } from 'model/objects/WalletCryptographyHandler';
import { sign } from 'tweetnacl';

/* used to simulate indexedDB database to test store/retrieve functions */
require('fake-indexeddb/auto');

/* mock crypto library object */
const crypto = {};

/**
 * This test uses a MOCK version of the crypto.subtle API.
 * The scope is not testing the SubtleCrypto library but testing the function
 * calls to it. It also tests the 'store' and 'retrieve' functions of indexedDB
 * with an intermediate simulation of app restart.
 */
describe('=== Wallet Cryptography Handler tests ===', () => {
  describe('encryption - decryption', () => {
    it('should correctly decrypt the previously encrypted ciphertext token', async () => {
      /* this is the MOCK crypto manager, it uses the mock SubtleCrypto library */
      // eslint-disable-next-line max-len
      let mockCryptoManager : WalletCryptographyHandler | null = new WalletCryptographyHandler(crypto);

      /* token to encrypt */
      const token = sign.keyPair().secretKey;

      /* adding to crypto used by mockCryptoManager the subtle property
         with the MOCKED functions: generateKey, encrypt, decrypt */
      Object.defineProperty(crypto, 'subtle', {
        value: {
          generateKey: () => ({
            publicKey: 'MOCK_PUBLIC_KEY',
            privateKey: 'MOCK_PRIVATE_KEY',
          }),
          // eslint-disable-next-line max-len
          encrypt: (s: AesGcmParams, key: CryptoKey, plaintext: Uint8Array) => new Promise((resolve) => {
            resolve(plaintext.buffer);
          }),
          // eslint-disable-next-line max-len
          decrypt: (s: RsaOaepParams, key: CryptoKey, ciphertext: BufferSource) => new Promise((resolve) => {
            resolve(ciphertext);
          }),
        },
      });

      /* initialization of wallet storage and crypto key in database */
      await mockCryptoManager.initWalletStorage();

      /* encrypting using mock encrypt */
      const cypher: ArrayBuffer = await mockCryptoManager.encrypt(token);

      /* deleting previous manager and creating new instance of wallet manager, this should ensure
         ensure the correct store and retrieve of the RSA key in indexedDB - app restart */
      mockCryptoManager = null;
      const newMockCryptoManager = new WalletCryptographyHandler(crypto);

      /* initialization of wallet storage and retrieve crypto key in database */
      await newMockCryptoManager.initWalletStorage();

      /* decrypting using mock decrypt */
      const plaintext:Uint8Array = new Uint8Array(await newMockCryptoManager.decrypt(cypher));

      expect(plaintext).toStrictEqual(token);
    });
  });
});
