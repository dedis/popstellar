import 'jest-extended';

import { Hash } from '../Hash';

test('Hash works against expected data - test vector 0 (ANSI)', () => {
  const hash: Hash = Hash.fromArray('abcd', '1234');
  expect(hash.toString()).toEqual('61I7DQkiMtdHFM5VygjbFqrVmn4NAl0wSVxkj6Q5iDw=');
});

test('Hash works against expected data - test vector 1 (UTF-8)', () => {
  const hash: Hash = Hash.fromArray('你们是真的', '好学生！');
  expect(hash.toString()).toEqual('bkkql8ZyOdbqrWY1QJHPGiz29zNMOEtaXXBHK1aWgjY=');
});

test('Hash works against expected data - test vector 2 (roll call)', () => {
  const hash: Hash = Hash.fromArray(
    'R', // roll call tag
    'u_y6BWJaedUb8C7xY2V9P1SC2ocaQkMymQgCX2SZGPo=', // lao id
    '1631871775', // timestamp
    'mon rôle call',
  ); // name
  expect(hash.toString()).toEqual('axL39-AXOH9nJnLEueyNI6Q-zbmZNSfOq5WOJSB8nyc=');
});

test('Hash equals work properly', () => {
  const hash1: Hash = Hash.fromArray('sameHash');
  const hash2: Hash = Hash.fromArray('sameHash');
  const hash3: Hash = Hash.fromArray('differentHash');

  expect(hash1.equals(hash2)).toBe(true);
  expect(hash1.equals(hash3)).toBe(false);
});

test('Hash from public key works properly', () => {
  const hash1: Hash = Hash.fromPublicKey('J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=');
  const hash2: Hash = Hash.fromPublicKey('oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms=');

  const expectedHash1 = '-_qR4IHwsiq50raa8jURNArds54=';
  const expectedHash2 = 'SGnNfF533PBEUMYPMqBSQY83z5U=';
  expect(hash1.valueOf()).toEqual(expectedHash1);
  expect(hash2.valueOf()).toEqual(expectedHash2);
});
