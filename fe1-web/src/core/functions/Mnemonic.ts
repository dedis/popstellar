import { randomInt, createHash } from 'crypto';

import base64url from 'base64url';
import * as bip39 from 'bip39';

import { Base64UrlData } from 'core/objects';

/**
 * This function computes the hashcode of a buffer according to the
 * Java Arrays.hashcode(bytearray) function to get the same results as in fe2.
 * @param buf
 * @returns computed hash code as a number
 */
function hashCode(buf: Buffer): number {
  if (buf === null) {
    return 0;
  }
  let result = 1;
  for (let element of buf) {
    if (element >= 2 ** 7) {
      element -= 2 ** 8;
    }
    result = 31 * result + (element == null ? 0 : element);
    result %= 2 ** 32;
    if (result >= 2 ** 31) {
      result -= 2 ** 32;
    }
    if (result < -(2 ** 31)) {
      result += 2 ** 32;
    }
  }
  return result;
}

/**
 * This function converts a buffer into mnemomic words from the english dictionary.
 * @param data buffer containing a base64 string
 * @returns array containing the mnemonic words
 */
function generateMnemonic(data: Buffer): string[] {
  try {
    const digest = createHash('sha256').update(data).digest();
    let mnemonic = '';
    bip39.setDefaultWordlist('english');
    mnemonic = bip39.entropyToMnemonic(digest);
    return mnemonic.split(' ').filter((word) => word.length > 0);
  } catch (e) {
    console.error(
      `Error generating the mnemonic for the base64 string ${base64url.encode(data)}`,
      e,
    );
    return [];
  }
}

/**
 * This function converts a buffer of a base64 string into a given number of mnemonic words.
 *
 * Disclaimer: there's no guarantee that different base64 inputs map to 2 different words. The
 * reason is that the representation space is limited. However, since the amount of messages is
 * low is practically improbable to have conflicts
 *
 * @param data Buffer of a base64 string
 * @param numberOfWords number of mnemonic words we want to generate
 * @return given number of mnemonic words concatenated with ' '
 */
function generateMnemonicFromBase64(data: Buffer, numberOfWords: number): string {
  // Generate the mnemonic words from the input data
  const mnemonicWords = generateMnemonic(data);
  if (mnemonicWords.length === 0) {
    return 'none';
  }

  let result = '';
  const hc = hashCode(data);
  for (let i = 0; i < numberOfWords; i += 1) {
    const wordIndex = Math.abs(hc + i) % mnemonicWords.length;
    result = `${result} ${mnemonicWords[wordIndex]}`;
  }

  return result.substring(1, result.length);
}

/**
 *  This function filters all non digits characters and returns the first nbDigits
 *  @param b64 base64 string containing numbers
 *  @param nbDigits numbers of digitis to extract from input
 *  @return string containing all the extracted numbers
 */
function getFirstNumberDigits(b64: string, nbDigits: number): string {
  const digits = b64.replace(/\D/g, '');
  return digits.slice(0, nbDigits).padStart(nbDigits, '0');
}

/**
 * This function generates a unique and memorable username from a base64 string.
 *
 * @param input base64 string.
 * @return a username composed of truncated mnemonic words and a numerical suffix.
 */
export function generateUsernameFromBase64(input: string): string {
  const words = generateMnemonicFromBase64(new Base64UrlData(input).toBuffer(), 2).split(' ');
  if (words.length < 2) {
    return `defaultUsername${randomInt(0, 10000000).toString().padStart(4, '0')}`;
  }
  const number = getFirstNumberDigits(input, 4);
  return `${words[0]}${words[1]}${number}`;
}
