import { makeEventsAliasMap, makeEventsMap, OpenedLaoStore } from 'store';
import { Hash, LaoEvent, WitnessSignature } from 'model/objects';

const MIN_WITNESS_FACTOR_N = 3; // numerator
const MIN_WITNESS_FACTOR_D = 5; // denominator, = three fifths = 60%

export function hasWitnessSignatureQuorum(witSigs: WitnessSignature[]): boolean {
  const lao = OpenedLaoStore.get();
  if (!lao) {
    return false;
  }

  const signaturesCount = witSigs.filter((witSig: WitnessSignature) =>
    lao.witnesses.includes(witSig.witness),
  ).length;

  return signaturesCount * MIN_WITNESS_FACTOR_D >= lao.witnesses.length * MIN_WITNESS_FACTOR_N;
}

const getEventMap = makeEventsMap();
const getEventAliases = makeEventsAliasMap();

/**
 * Retrieves the event id associated with a given alias
 *
 * @param state the store state
 * @param id the id (or alias) to be found
 *
 * @returns LaoEvent associated with the id, if found
 * @returns undefined if the id doesn't match any known event ID or alias
 */
export function getEventFromId(state: any, id: Hash): LaoEvent | undefined {
  const eventAlias = getEventAliases(state);
  const eventMap = getEventMap(state);

  const idStr = id.valueOf();
  const evtId = idStr in eventAlias ? eventAlias[idStr] : idStr;

  return evtId in eventMap ? eventMap[evtId] : undefined;
}
