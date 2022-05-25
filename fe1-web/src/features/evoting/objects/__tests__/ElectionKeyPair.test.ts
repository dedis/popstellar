import { ElectionKeyPair } from '../ElectionKeyPair';

describe('ElectionKeyPair', () => {
  it('can generate random key pair', () => {
    expect(() => ElectionKeyPair.generate()).not.toThrow();
  });

  it('can en- and decrypt a message', () => {
    const keyPair = ElectionKeyPair.generate();
    const data = 'x';
    const encryptedData = keyPair.publicKey.encrypt(Buffer.from(data, 'utf-8'));
    const decryptedData = keyPair.privateKey.decrypt(encryptedData).toString('utf-8');

    console.log('publicKey', keyPair.publicKey.toBase64());
    console.log('privateKey', keyPair.privateKey.toBase64());

    expect(decryptedData).toEqual(data);
  });
});
