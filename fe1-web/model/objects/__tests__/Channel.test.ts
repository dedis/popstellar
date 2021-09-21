import 'jest-extended';

import { Hash } from '../Hash';
import { channelFromIds } from '../Channel';

test('Channel should return channels properly', () => {
  const id: Hash = Hash.fromStringArray('id');

  expect(channelFromIds()).toEqual('/root');
  expect(channelFromIds(id)).toEqual(`/root/${id}`);
});
