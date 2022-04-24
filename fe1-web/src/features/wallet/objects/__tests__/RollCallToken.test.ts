import { mockKeyPair, mockLao, mockRC } from '__tests__/utils';
import { PopToken } from 'core/objects';

import { RollCallToken } from '../RollCallToken';

const mockToken = PopToken.fromState(mockKeyPair.toState());

describe('Roll call token object', () => {
  it('throws when token is undefined', () => {
    expect(() => new RollCallToken({ token: undefined })).toThrow();
  });
  it('throws when laoId is undefined', () => {
    expect(() => new RollCallToken({ token: mockToken })).toThrow();
  });
  it('throws when rollCallId is undefined', () => {
    expect(() => new RollCallToken({ token: mockToken, laoId: mockLao.id })).toThrow();
  });
  it('throws when rollCallName is undefined', () => {
    expect(
      () => new RollCallToken({ token: mockToken, laoId: mockLao.id, rollCallId: mockRC.id }),
    ).toThrow();
  });
});
