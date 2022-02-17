import 'jest-extended';

import { Lao, LaoState } from '../Lao';

test('Lao can do a state round-trip', () => {
  const laoState: LaoState = {
    id: '1234',
    name: 'MyLao',
    creation: 123,
    last_modified: 1234,
    organizer: '1234',
    witnesses: [],
  };

  const lao: Lao = Lao.fromState(laoState);

  expect(lao.toState()).toStrictEqual(laoState);
});
