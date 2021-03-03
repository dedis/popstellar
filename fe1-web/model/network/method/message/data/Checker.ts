import {
  Base64Data, Hash, PublicKey, Timestamp, WitnessSignature,
} from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';

const TIMESTAMP_BASE_TIME = new Timestamp(1577833200); // 1st january 2020

export function checkTimestampStaleness(timestamp: Timestamp) {
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

export function checkWitnessSignatures(witSig: WitnessSignature[], data: Base64Data) {
  if (!witSig.every((ws) => ws.verify(data))) {
    throw new ProtocolError("Invalid 'witness_signatures' parameter encountered: invalid signature");
  }
}

export function checkModificationId(id: Hash) {
  // FIXME check modification id
  return id; // simply to remove eslint warning
}

export function checkModificationSignatures(sigPairs: WitnessSignature[]) {
  // FIXME check modification signatures
  return sigPairs; // simply to remove eslint warning
}
