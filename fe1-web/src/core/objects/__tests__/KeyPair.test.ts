import 'jest-extended';
// @ts-ignore
import testKeyPair from 'test_data/keypair.json';

import { Base64UrlData, KeyPair, KeyPairState } from '../index';

const kpState: KeyPairState = {
  publicKey: testKeyPair.publicKey,
  privateKey: testKeyPair.privateKey,
};

test('KeyPair should encode and decode properly', () => {
  const kp: KeyPair = KeyPair.fromState(kpState);
  expect(kp.toState()).toStrictEqual(kpState);
});

test('KeyPair can sign and verify', () => {
  const kp: KeyPair = KeyPair.fromState(kpState);
  const message: Base64UrlData = Base64UrlData.encode('message');
  const signature = kp.privateKey.sign(message);
  expect(signature.verify(kp.publicKey, message)).toBeTrue();
});

test('Invalid signature gets rejected', () => {
  const kp: KeyPair = KeyPair.fromState(kpState);
  const message: Base64UrlData = Base64UrlData.encode('message');
  const other: Base64UrlData = Base64UrlData.encode('other message');
  const signature = kp.privateKey.sign(message);
  expect(signature.verify(kp.publicKey, other)).toBeFalse();
});

test('Invalid public key gets rejected', () => {
  const kp: KeyPair = KeyPair.fromState(kpState);
  const message: Base64UrlData = Base64UrlData.encode('message');
  const signature = kp.privateKey.sign(message);
  expect(signature.verify(kp.privateKey, message)).toBeFalse();
});
