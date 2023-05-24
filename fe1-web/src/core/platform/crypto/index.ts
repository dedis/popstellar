import { get, set } from '../Storage';
import { getSubtleCrypto } from './browser';

const algorithm: RsaHashedKeyAlgorithm = {
  name: 'RSA-OAEP',
  modulusLength: 4096,
  publicExponent: new Uint8Array([1, 0, 1]),
  hash: { name: 'SHA-256' },
};

const keyUsages: KeyUsage[] = ['encrypt', 'decrypt'];

// Identifier in IndexedDB for the KeyPair storage
const keysDbId: string = 'SecureKeyPair';

/**
 * Generates the key pair
 * @returns the CryptoKeyPair
 * @private
 */
async function createKeyPair(): Promise<CryptoKeyPair> {
  const keyPair = await getSubtleCrypto().generateKey(algorithm, false, keyUsages);

  if (!keyPair) {
    throw new Error('KeyPair generation failed');
  }

  return keyPair;
}

/**
 * Retrieves the key pair from IndexedDB, creates and stores it as needed
 * @returns the CryptoKeyPair
 * @private
 */
async function getOrCreateKeyPair(): Promise<CryptoKeyPair> {
  let keys: CryptoKeyPair = await get(keysDbId);
  if (keys === undefined) {
    keys = await createKeyPair();
    await set(keysDbId, keys);
  }
  return keys;
}

/**
 * Encrypts the given data with a platform-specific, secure encryption mechanism.
 * @param plaintext Uint8Array or ArrayBuffer representation of the plaintext
 * @returns the ciphertext
 *
 * @remark
 * The Web-based implementation uses RSA-OAEP with a 4096 bit modulus and SHA256 hashing.
 * The key is not-exportable and persisted within IndexedDB.
 */
export async function encrypt(plaintext: Uint8Array | ArrayBuffer): Promise<ArrayBuffer> {
  const keys: CryptoKeyPair = await getOrCreateKeyPair();
  if (!keys.publicKey) {
    throw new Error('Public key could not be retrieved or generated');
  }

  return getSubtleCrypto().encrypt(algorithm, keys.publicKey, plaintext);
}

/**
 * Decrypts the given ciphertext with a platform-specific, secure decryption mechanism.
 * @param ciphertext Uint8Array or ArrayBuffer representation of the ciphertext
 * @returns the plaintext
 *
 * @remark
 * The Web-based implementation uses RSA-OAEP with a 4096 bit modulus and SHA256 hashing.
 * The key is not-exportable and persisted within IndexedDB.
 */
export async function decrypt(ciphertext: Uint8Array | ArrayBuffer): Promise<ArrayBuffer> {
  const keys: CryptoKeyPair = await getOrCreateKeyPair();
  if (!keys.privateKey) {
    throw new Error('Private key could not be retrieved or generated');
  }

  return getSubtleCrypto().decrypt(algorithm, keys.privateKey, ciphertext);
}

/**
 * Irretrievably delete any cryptographic material from the device.
 *
 * @remark
 * It will become impossible to load the user's data automatically.
 * Unless the user has a backup of his wallet seed, all LAO tokens
 * and the user's identity will be irretrievably lost.
 */
export async function wipeOutDangerously(): Promise<void> {
  await set(keysDbId, null);
}

export default {
  encrypt,
  decrypt,
};
