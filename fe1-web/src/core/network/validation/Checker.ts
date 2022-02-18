import { Base64UrlData, Hash, PublicKey, Timestamp, ProtocolError } from 'core/objects';
import { Lao } from 'features/lao/objects';
import { WitnessSignature } from 'features/witness/objects';

export function checkTimestampStaleness(timestamp: Timestamp) {
  const TIMESTAMP_BASE_TIME = new Timestamp(1577833200); // 1st january 2020
  if (timestamp.before(TIMESTAMP_BASE_TIME)) {
    throw new ProtocolError('Invalid timestamp encountered: stale timestamp');
  }
}

export function checkWitnesses(witnesses: PublicKey[]) {
  if (witnesses.length !== [...new Set(witnesses)].length) {
    throw new ProtocolError("Invalid 'witnesses' parameter encountered: duplicate witness keys");
  }
}

export function checkAttendees(attendees: PublicKey[]) {
  if (attendees.length !== [...new Set(attendees)].length) {
    throw new ProtocolError("Invalid 'attendees' parameter encountered: duplicate attendees keys");
  }
}

export function checkWitnessSignatures(witSig: WitnessSignature[], data: Base64UrlData) {
  if (!witSig.every((ws) => ws.verify(data))) {
    throw new ProtocolError('Invalid witness signatures parameter encountered: invalid signature');
  }
}

export function checkModificationId(id: Hash) {
  // FIXME check modification id
  return id; // simply to remove eslint warning
}

const MIN_WITNESS_FACTOR_N = 3; // numerator
const MIN_WITNESS_FACTOR_D = 5; // denominator, = three fifths = 60%

export function hasWitnessSignatureQuorum(witSigs: WitnessSignature[], lao: Lao): boolean {
  if (!lao) {
    return false;
  }

  const signaturesCount = witSigs.filter((witSig: WitnessSignature) =>
    lao.witnesses.includes(witSig.witness),
  ).length;

  return signaturesCount * MIN_WITNESS_FACTOR_D >= lao.witnesses.length * MIN_WITNESS_FACTOR_N;
}
