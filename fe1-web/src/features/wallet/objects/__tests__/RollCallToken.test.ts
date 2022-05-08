import { mockKeyPair, mockLao, mockRC } from '__tests__/utils';
import { PopToken } from 'core/objects';

import { RollCallToken } from '../RollCallToken';

const mockToken = PopToken.fromState(mockKeyPair.toState());

describe('Roll call token object', () => {
  it('can build a defined object', () => {
    expect(
      () =>
        new RollCallToken({
          token: mockToken,
          laoId: mockLao.id,
          rollCallId: mockRC.id,
          rollCallName: mockRC.name,
        }),
    ).not.toThrow(Error);
  });

  it('throws when token is undefined', () => {
    expect(() => new RollCallToken({ token: undefined })).toThrow(Error);
  });

  it('throws when laoId is undefined', () => {
    expect(() => new RollCallToken({ token: mockToken })).toThrow(Error);
  });

  it('throws when rollCallId is undefined', () => {
    expect(() => new RollCallToken({ token: mockToken, laoId: mockLao.id })).toThrow(Error);
  });

  it('throws when rollCallName is undefined', () => {
    expect(
      () => new RollCallToken({ token: mockToken, laoId: mockLao.id, rollCallId: mockRC.id }),
    ).toThrow(Error);
  });
});
