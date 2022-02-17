import { Hash } from '../Hash';

/* The following constants are used (and common) in all derivation paths
 * example path: m / 888 / 0' / LAO_id' / roll_call_id */
const PREFIX: string = 'm';
const PURPOSE: string = '888';
const ACCOUNT: string = '0';
const PATH_SEPARATOR: string = '/';
const HARDENED_SYMBOL: string = "'";

/**
 * Transforms a Hash into BIP-32 derivation path/segments.
 * @param hash to be used for derivation
 *
 * @remarks
 *
 * The generated path is inefficient as it only stores 24 bits per segment, instead of 31 bits.
 * However, the path is mutually compatible with the (similarly-inefficient) Java implementation
 */
function segmentsFromHash(hash: Hash): string {
  const buffer = hash.toBuffer();

  let segments: string[] = [];
  for (let i = 0; i < buffer.length; i += 3) {
    const seg = buffer.slice(i, i + 3).join('') + HARDENED_SYMBOL;
    segments = segments.concat(seg);
  }
  return segments.join(PATH_SEPARATOR);
}

/**
 * Transform a LAO id and RollCall id into a BIP-32 derivation path.
 * @param laoId the id of the LAO
 * @param rollCallId the id of the Roll Call
 */
export function fromLaoRollCall(laoId: Hash, rollCallId: Hash): string {
  return [
    PREFIX,
    PURPOSE.concat(HARDENED_SYMBOL),
    ACCOUNT.concat(HARDENED_SYMBOL),
    segmentsFromHash(laoId),
    segmentsFromHash(rollCallId),
  ].join(PATH_SEPARATOR);
}
