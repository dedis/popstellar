import { Hash, ProtocolError, PublicKey, Timestamp, WitnessSignature } from 'core/objects';

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

export function checkWitnessSignatures(witSig: WitnessSignature[], data: Hash) {
  if (!witSig.every((ws) => ws.verify(data))) {
    throw new ProtocolError('Invalid witness signatures parameter encountered: invalid signature');
  }
}

/**
 * Verify if the witness signatures constitute a quorum of the LAO witnesses
 *
 * @param witSigs - The message witness signatures
 * @param lao - A LAO providing a list of witness keys
 */
export function hasWitnessSignatureQuorum(
  witSigs: WitnessSignature[],
  lao: { witnesses: PublicKey[] },
): boolean {
  if (!lao) {
    return false;
  }

  const MIN_WITNESS_FACTOR_N = 3; // numerator
  const MIN_WITNESS_FACTOR_D = 5; // denominator, = three fifths = 60%

  const signaturesCount = witSigs.filter((witSig: WitnessSignature) =>
    lao.witnesses.includes(witSig.witness),
  ).length;

  return signaturesCount * MIN_WITNESS_FACTOR_D >= lao.witnesses.length * MIN_WITNESS_FACTOR_N;
}
