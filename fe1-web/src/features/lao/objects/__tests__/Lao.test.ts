import 'jest-extended';

import { mockLaoState } from '__tests__/utils/TestUtils';

import { Lao } from '../Lao';

test('Lao can do a state round-trip', () => {
  const lao: Lao = Lao.fromState(mockLaoState);
  expect(lao.toState()).toStrictEqual(mockLaoState);
});
