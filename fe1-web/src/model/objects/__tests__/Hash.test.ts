import 'jest-extended';

import { Hash } from '../Hash';

test('Hash works against expected data - test vector 0 (ANSI)', () => {
  const hash: Hash = Hash.fromStringArray('abcd', '1234');
  expect(hash.toString()).toEqual('61I7DQkiMtdHFM5VygjbFqrVmn4NAl0wSVxkj6Q5iDw=');
});

test('Hash works against expected data - test vector 1 (UTF-8)', () => {
  const hash: Hash = Hash.fromStringArray('你们是真的', '好学生！');
  expect(hash.toString()).toEqual('bkkql8ZyOdbqrWY1QJHPGiz29zNMOEtaXXBHK1aWgjY=');
});

test('Hash works against expected data - test vector 2 (roll call)', () => {
  const hash: Hash = Hash.fromStringArray(
    'R', // roll call tag
    'u_y6BWJaedUb8C7xY2V9P1SC2ocaQkMymQgCX2SZGPo=', // lao id
    '1631871775', // timestamp
    'mon rôle call',
  ); // name
  expect(hash.toString()).toEqual('axL39-AXOH9nJnLEueyNI6Q-zbmZNSfOq5WOJSB8nyc=');
});

test('Hash equals work properly', () => {
  const hash1: Hash = Hash.fromStringArray('sameHash');
  const hash2: Hash = Hash.fromStringArray('sameHash');
  const hash3: Hash = Hash.fromStringArray('differentHash');

  expect(hash1.equals(hash2)).toBe(true);
  expect(hash1.equals(hash3)).toBe(false);
});
