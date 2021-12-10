import { derivePath, getPublicKey } from 'ed25519-hd-key';
import { EventStore, OpenedLaoStore, WalletStore } from 'store';
import { Hash } from '../Hash';
import { PopToken } from '../PopToken';
import { Base64UrlData } from '../Base64Url';
import { PublicKey } from '../PublicKey';
import { PrivateKey } from '../PrivateKey';
import * as bip39path from './Bip32Path';
import { RollCall } from '../RollCall';

/**
 * Generates a token for an arbitrary derivation path
 * @param path the key derivation path
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

  return new PopToken({
    publicKey: new PublicKey(Base64UrlData.fromBuffer(pubKey).valueOf()),
    privateKey: new PrivateKey(Base64UrlData.fromBuffer(key).valueOf()),
  });
}

/**
 * Generates a token for a given LAOId and RollCallId
 * @param laoId the id of the LAO
 * @param rollCallId the id of the Roll Call
 * @returns a Promise resolving to a PopToken
 */
export function generateToken(laoId: Hash, rollCallId: Hash): Promise<PopToken> {
  const path = bip39path.fromLaoRollCall(laoId, rollCallId);
  return generateTokenFromPath(path);
}

/**
 * Retrieve the latest PoP token associated with the current LAO from the store.
 *
 * @remarks
 * Do not use it inside of a react component, this is meant to be used only where you cannot
 * access to reducers.
 *
 * @returns A Promise that resolves to a PoP token or to undefined if no token exists
 */
export async function getCurrentPopTokenFromStore(): Promise<PopToken | undefined> {
  const lao = OpenedLaoStore.get();

  const rollCallId = lao.last_tokenized_roll_call_id;
  console.log(rollCallId);
  if (rollCallId === undefined) {
    return undefined;
  }

  const rollCall = EventStore.getEvent(rollCallId) as RollCall;
  const token = await generateToken(lao.id, rollCallId);
  console.log(token.publicKey);
  if (rollCall.containsToken(token)) {
    console.log(token);
    return token;
  }
  return undefined;
}
