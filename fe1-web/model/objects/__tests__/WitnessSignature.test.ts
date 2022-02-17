import 'jest-extended';
import testKeyPair from 'test_data/keypair.json';
import { Hash } from '../Hash';
import { KeyPair } from '../KeyPair';
import { WitnessSignature, WitnessSignatureState } from '../WitnessSignature';

const kp = KeyPair.fromState({
  publicKey: testKeyPair.publicKey,
  privateKey: testKeyPair.privateKey,
});

const messageId: Hash = Hash.fromStringArray('messageId');

test('WitnessScreen signature verify works correctly', () => {
  const witnessSignature: WitnessSignature = new WitnessSignature({
    witness: kp.publicKey,
    signature: kp.privateKey.sign(messageId),
  });

  expect(witnessSignature.verify(messageId)).toBeTrue();
});

test('WitnessScreen signature verify fails correctly', () => {
  const witnessSignature: WitnessSignature = new WitnessSignature({
    witness: kp.publicKey,
    signature: kp.privateKey.sign(messageId),
  });

  const otherId = Hash.fromStringArray('wrongMessage');

  expect(witnessSignature.verify(otherId)).toBeFalse();
});

test('WitnessScreen signature should encode and decode properly', () => {
  const witSigState: WitnessSignatureState = {
    witness: kp.publicKey.valueOf(),
    signature: kp.privateKey.sign(messageId).valueOf(),
  };

  expect(WitnessSignature.fromJson(witSigState).toState()).toEqual(witSigState);
});
