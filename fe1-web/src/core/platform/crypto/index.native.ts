/**
 * Encrypts the given data with a platform-specific, secure encryption mechanism.
 * @param plaintext Uint8Array or ArrayBuffer representation of the plaintext
 * @returns the ciphertext
 *
 * @remark
 * TODO: How is the native encryption done, at the moment not at all
 */
export async function encrypt(plaintext: Uint8Array | ArrayBuffer): Promise<ArrayBuffer> {
  return plaintext;
}

/**
 * Decrypts the given ciphertext with a platform-specific, secure decryption mechanism.
 * @param ciphertext Uint8Array or ArrayBuffer representation of the ciphertext
 * @returns the plaintext
 *
 * @remark
 * TODO: How is the native decryption done, at the moment not at all
 */
export async function decrypt(ciphertext: Uint8Array | ArrayBuffer): Promise<ArrayBuffer> {
  return ciphertext;
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
  // await set(keysDbId, null);
}

export default {
  encrypt,
  decrypt,
};
