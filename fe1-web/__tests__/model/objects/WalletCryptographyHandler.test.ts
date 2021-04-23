import 'jest-extended';
import { WalletCryptographyHandler } from 'model/objects/WalletCryptographyHandler';
import { sign } from 'tweetnacl';
import STRINGS from 'res/strings';
import { get } from 'idb-keyval';

/* used to simulate indexedDB database to test store/retrieve functions */
require('fake-indexeddb/auto');

/* mock crypto library object */
const crypto = {};

/* adding to crypto used by mockCryptoManager the subtle property
         with the MOCKED functions: generateKey, encrypt, decrypt */
Object.defineProperty(crypto, 'subtle', {
  value: {
    generateKey: () => ({
      publicKey: sign.keyPair().secretKey,
      privateKey: sign.keyPair().publicKey,
    }),

    /* this mock encrypt function appends public encryption key to seed */
    encrypt: async (s: AesGcmParams, publicKey: CryptoKey, plaintext: Uint8Array) => {
      const ciphertext: string = plaintext.join().concat(publicKey.toString());
      const ciphertextBuffer: ArrayBuffer = Buffer.from(ciphertext);
      return new Promise((resolve) => {
        resolve(ciphertextBuffer);
      });
    },

    /* the mock decrypt function is called AFTER walletCryptoManager destruction.
       The check is done on key coherence in database: since the encryption appended
       the public key to the seed we need to retrieve the public key and undo the append
       in order to provide correct decryption */
    decrypt: async (s: RsaOaepParams, privateKey: CryptoKey, ciphertext: ArrayBuffer) => {
      /* note the publicKey has to be retrieved here since the one passed in
      parameter by the decrypt call in handler is the private key */
      const publicKey = await get(STRINGS.wallet_public_key_id);
      /* undoing public key append by substituting the substring with empty string */
      const plaintext: string = ciphertext.toString().replace(publicKey.toString(), '');
      const plaintextBuffer: ArrayBuffer = Buffer.from(plaintext);
      return new Promise((resolve) => {
        resolve(plaintextBuffer);
      });
    },
  },
});

/**
 * This test uses a MOCK version of the crypto.subtle API.
 * The scope is not testing the SubtleCrypto library but testing the function
 * calls to it. It also tests the 'store' and 'retrieve' functions of indexedDB
 * with an intermediate simulation of app restart.
 */
describe('=== Wallet Cryptography Handler tests ===', () => {
  describe('encryption - decryption', () => {
    it('should correctly encrypt and decrypt the generated seed', async () => {
      /* this is the MOCK crypto manager, it uses the mock SubtleCrypto library */
      // eslint-disable-next-line max-len
      let mockCryptoManager : WalletCryptographyHandler | null = new WalletCryptographyHandler(crypto as Crypto);

      /* seed to encrypt */
      const seed = sign.keyPair().secretKey;

      /* initialization of wallet storage and crypto key in database
       here the generated keys are STORED in indexedDB simulator */
      await mockCryptoManager.initWalletStorage();

      /* encrypting using mock encrypt */
      const cypher: ArrayBuffer = await mockCryptoManager.encrypt(seed);

      /* deleting previous manager and creating new instance of wallet manager, this should ensure
         ensure the correct store and retrieve of the RSA key in indexedDB - app restart */
      mockCryptoManager = null;
      const newMockCryptoManager = new WalletCryptographyHandler(crypto as Crypto);

      expect(mockCryptoManager).toBeNull();

      /* initialization of wallet storage and retrieve crypto key in database */
      await newMockCryptoManager.initWalletStorage();

      /* decrypting using mock decrypt, if the decryption works it implies indexedDB
         database has correctly stored the keys after app restart */
      const plaintext: ArrayBuffer = await newMockCryptoManager.decrypt(cypher);

      expect(plaintext.toString()).toStrictEqual(seed.join());
    });
  });
});
