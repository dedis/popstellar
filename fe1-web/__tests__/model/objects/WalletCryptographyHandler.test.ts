import 'jest-extended';
import { WalletCryptographyHandler } from 'model/objects/WalletCryptographyHandler';
import { sign } from 'tweetnacl';

require('fake-indexeddb/auto');

describe('=== Wallet Cryptography Handler tests ===', () => {
  describe('encryption - decryption', () => {
    it('should correctly decrypt the previously encrypted ciphertext token', async () => {
      const cryptoManager = new WalletCryptographyHandler();
      await cryptoManager.initWalletStorage();

      const token = sign.keyPair().secretKey;
      const cypher: ArrayBuffer = await cryptoManager.encrypt(token);
      const plaintext = await cryptoManager.decrypt(cypher);

      expect(plaintext).toBe(token);
    });
  });

  describe('encryption - re-init of wallet object - decryption', () => {
    it('should correctly decrypt the previously encrypted ciphertext token passing through a wallet object destruction and re-construction', async () => {
      const cryptoManager = new WalletCryptographyHandler();
      await cryptoManager.initWalletStorage();

      const token = sign.keyPair().secretKey;
      const cypher: ArrayBuffer = await cryptoManager.encrypt(token);

      // destroy wallet

      const plaintext = await cryptoManager.decrypt(cypher);

      expect(plaintext).toBe(token);
    });
  });
});
