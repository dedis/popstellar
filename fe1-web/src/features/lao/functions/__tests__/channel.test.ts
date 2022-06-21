import { mockLaoId } from '__tests__/utils';
import { ROOT_CHANNEL } from 'core/objects';

import { getLaoChannel } from '../channel';

describe('getLaoChannel', () => {
  it('should return the channel, given a valid lao id', () => {
    const channel = getLaoChannel(mockLaoId);
    expect(channel).toEqual(`${ROOT_CHANNEL}/${mockLaoId}`);
  });
});
