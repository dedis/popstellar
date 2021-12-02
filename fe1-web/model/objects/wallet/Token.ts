import { derivePath, getPublicKey } from 'ed25519-hd-key';
import { getKeyPairState, getStore, makeCurrentLao, makeEventGetter, WalletStore } from 'store';
import { useSelector } from 'react-redux';
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
 * Retrieve the latest PoP token associated with the current LAO
 * @returns A Promise that resolves to a PoP token or to undefined if no token exists
 */
export async function getCurrentPopToken(): Promise<PopToken | undefined> {
  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);
  if (lao === undefined) {
    return undefined;
  }

  const rollCallId = lao.last_tokenized_roll_call_id;
  if (rollCallId === undefined) {
    return undefined;
  }

  const eventSelect = makeEventGetter(lao.id, rollCallId);
  const rollCall: RollCall = useSelector(eventSelect) as RollCall;
  const token = await generateToken(lao.id, rollCallId);
  if (rollCall.containsToken(token)) {
    return token;
  }
  return undefined;
}

/**
 * Returns the current public key of the current user.
 */
export async function getCurrentPublicKey(): Promise<PublicKey | undefined> {
  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);
  if (lao === undefined) {
    console.log('Undefined lao');
    return undefined;
  }

  // If the current user is the organizer, return his public key
  const publicKeyString = getKeyPairState(getStore().getState()).keyPair?.publicKey;
  if (publicKeyString && publicKeyString === lao.organizer.valueOf()) {
    console.log('Organizer');
    return new PublicKey(publicKeyString);
  }

  // Otherwise, get the pop token of the attendee using the last tokenized roll call
  const rollCallId = lao.last_tokenized_roll_call_id;
  if (rollCallId === undefined) {
    console.log('Undefined roll call');
    return undefined;
  }

  console.log('Attendee');
  const eventSelect = makeEventGetter(lao.id, rollCallId);
  const rollCall: RollCall = useSelector(eventSelect) as RollCall;
  const token = await generateToken(lao.id, rollCallId);
  if (rollCall.containsToken(token)) {
    return token.publicKey;
  }
  return undefined;
}
