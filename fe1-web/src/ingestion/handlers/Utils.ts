import { OpenedLaoStore } from 'store';
import { WitnessSignature } from 'model/objects';

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
