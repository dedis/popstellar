import 'jest-extended';

import { Hash } from '../Hash';

test('Hash works against expected data', () => {
  const hash: Hash = Hash.fromStringArray('abcd', '1234');
  expect(hash.toString()).toEqual('61I7DQkiMtdHFM5VygjbFqrVmn4NAl0wSVxkj6Q5iDw=');
});

test('Hash equals work properly', () => {
  const hash1: Hash = Hash.fromStringArray('sameHash');
  const hash2: Hash = Hash.fromStringArray('sameHash');
  const hash3: Hash = Hash.fromStringArray('differentHash');

  expect(hash1.equals(hash2)).toBe(true);
  expect(hash1.equals(hash3)).toBe(false);
});
