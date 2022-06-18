import { mockKeyPair, mockLao } from '__tests__/utils';
import { OmitMethods } from 'core/types';
import { mockRollCall } from 'features/rollCall/__tests__/utils';

import { PopToken } from '../PopToken';
import { RollCallToken } from '../RollCallToken';

const mockToken = PopToken.fromState(mockKeyPair.toState());

describe('Roll call token object', () => {
  it('can build a defined object', () => {
    expect(
      () =>
        new RollCallToken({
          token: mockToken,
          laoId: mockLao.id,
          rollCallId: mockRollCall.id,
          rollCallName: mockRollCall.name,
        }),
    ).not.toThrow(Error);
  });

  it('throws when token is undefined', () => {
    expect(
      () => new RollCallToken({ token: undefined } as unknown as OmitMethods<RollCallToken>),
    ).toThrow(Error);
  });

  it('throws when laoId is undefined', () => {
    expect(
      () => new RollCallToken({ token: mockToken } as unknown as OmitMethods<RollCallToken>),
    ).toThrow(Error);
  });

  it('throws when rollCallId is undefined', () => {
    expect(
      () =>
        new RollCallToken({
          token: mockToken,
          laoId: mockLao.id,
        } as unknown as OmitMethods<RollCallToken>),
    ).toThrow(Error);
  });

  it('throws when rollCallName is undefined', () => {
    expect(
      () =>
        new RollCallToken({
          token: mockToken,
          laoId: mockLao.id,
          rollCallId: mockRollCall.id,
        } as unknown as OmitMethods<RollCallToken>),
    ).toThrow(Error);
  });
});
