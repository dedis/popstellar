import * as crypto from 'crypto';

import base64url from 'base64url';
import * as bip39 from 'bip39';

import { Base64UrlData } from 'core/objects';

function hashCode(a: Buffer): number {
  if (a === null) {
    return 0;
  }
  let result = 1;
  for (let element of a) {
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

function generateMnemonic(data: Buffer): string[] {
  try {
    const digest = crypto.createHash('sha256').update(data).digest();
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

export const generateMnemonicWordFromBase64 = (input: string, numberOfWords: number): string => {
  return generateMnemonicFromBase64(new Base64UrlData(input).toBuffer(), numberOfWords);
};
