import 'jest-extended';

import { Hash } from '../Hash';
import { channelFromId } from '../Channel';

test('Channel should return channels properly', () => {
  const id: Hash = Hash.fromStringArray('id');

  expect(channelFromId()).toEqual('/root');
  expect(channelFromId(id)).toEqual(`/root/${id}`);
});
