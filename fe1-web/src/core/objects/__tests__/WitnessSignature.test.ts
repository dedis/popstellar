import 'jest-extended';

import testKeyPair from 'test_data/keypair.json';

import { Hash, KeyPair, WitnessSignature, WitnessSignatureState } from '../index';

const kp = KeyPair.fromState({
  publicKey: testKeyPair.publicKey,
  privateKey: testKeyPair.privateKey,
});

const messageId: Hash = Hash.fromArray('messageId');

test('Witness signature verify works correctly', () => {
  const witnessSignature: WitnessSignature = new WitnessSignature({
    witness: kp.publicKey,
    signature: kp.privateKey.sign(messageId),
  });

  expect(witnessSignature.verify(messageId)).toBeTrue();
});

test('Witness signature verify fails correctly', () => {
  const witnessSignature: WitnessSignature = new WitnessSignature({
    witness: kp.publicKey,
    signature: kp.privateKey.sign(messageId),
  });

  const otherId = Hash.fromArray('wrongMessage');

  expect(witnessSignature.verify(otherId)).toBeFalse();
});

test('Witness signature should encode and decode properly', () => {
  const witSigState: WitnessSignatureState = {
    witness: kp.publicKey.valueOf(),
    signature: kp.privateKey.sign(messageId).valueOf(),
  };

  expect(WitnessSignature.fromState(witSigState).toState()).toEqual(witSigState);
});
