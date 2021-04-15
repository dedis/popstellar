import 'jest-extended';
import { WalletCryptographyHandler } from 'model/objects/WalletCryptographyHandler';
import { sign } from 'tweetnacl';

require('fake-indexeddb/auto');

Object.defineProperty(global.self, 'crypto', {
  value: {
    subtle: window.crypto.subtle,
  },

});

describe('=== Wallet Cryptography Handler tests ===', () => {
  describe('encryption - decryption', () => {
    it('should correctly decrypt the previously encrypted ciphertext token', async () => {
      const cryptoManager = new WalletCryptographyHandler();
      await cryptoManager.initWalletStorage();

      const token = sign.keyPair().secretKey;
      const cypher: ArrayBuffer = await cryptoManager.encrypt(token);

      // destroy wallet cryptography manager object

      const plaintext = await cryptoManager.decrypt(cypher);

      expect(plaintext).toBe(token);
    });
  });
});
