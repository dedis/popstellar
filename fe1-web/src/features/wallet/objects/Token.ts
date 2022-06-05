import { derivePath, getPublicKey } from 'ed25519-hd-key';

import { Base64UrlData, Hash, PopToken, PrivateKey, PublicKey } from 'core/objects';

import { WalletFeature } from '../interface';
import { WalletStore } from '../store';
import * as bip39path from './Bip32Path';

/**
 * Generates a token for an arbitrary derivation path.
 *
 * @param path - The key derivation path
 * @returns a Promise resolving to a PopToken
 * @private
 *
 * @remarks
 * This is only exported for testing reasons
 */
export async function generateTokenFromPath(path: string): Promise<PopToken> {
  const seedArray = await WalletStore.getSeed();
  const hexSeed = Buffer.from(seedArray).toString('hex');

  const { key } = derivePath(path, hexSeed);
  const pubKey = getPublicKey(key, false);
  const privateKeyBuffer = Buffer.concat([key, pubKey]);

  return new PopToken({
    publicKey: new PublicKey(Base64UrlData.fromBuffer(pubKey).valueOf()),
    privateKey: new PrivateKey(Base64UrlData.fromBuffer(privateKeyBuffer).valueOf()),
  });
}

/**
 * Generates a token for a given LAOId and RollCallId if the RollCallId exists.
 *
 * @param laoId - The id of the LAO
 * @param rollCallId - The id of the Roll Call
 * @returns a Promise resolving to a PopToken or to undefined
 */
export function generateToken(laoId: Hash, rollCallId: Hash | undefined): Promise<PopToken> {
  if (rollCallId === undefined) {
    return Promise.reject(new Error('Cannot generate a token with an undefined roll call id'));
  }
  const path = bip39path.fromLaoRollCall(laoId, rollCallId);
  return generateTokenFromPath(path);
}

/**
 * Retrieve the latest PoP token associated with the current LAO from the store.
 * @param getCurrentLao A function returning the current lao
 * @param getRollCallById A function to get a roll call by its id
 * @remarks
 * Do not use it inside of a react component, this is meant to be used only where you
 * do not have access to reducers.
 *
 * @returns A Promise that resolves to a PoP token or to undefined if no token exists
 */
export const getCurrentPopTokenFromStore =
  (
    getCurrentLao: () => WalletFeature.Lao,
    getRollCallById: (id: Hash) => WalletFeature.RollCall | undefined,
  ) =>
  async (): Promise<PopToken> => {
    const lao = getCurrentLao();

    const rollCallId = lao.last_tokenized_roll_call_id;
    if (rollCallId === undefined) {
      throw new Error('Cannot retrieve pop token: roll call id is undefined');
    }

    const rollCall = getRollCallById(rollCallId);
    if (!rollCall) {
      throw new Error(`Cannot retrieve pop token: did not find a roll call with id ${rollCallId}`);
    }

    const token = await generateToken(lao.id, rollCallId);

    if (!rollCall.containsToken(token)) {
      throw new Error('Cannot retrieve pop token: roll call does not contain this token');
    }

    return token;
  };
