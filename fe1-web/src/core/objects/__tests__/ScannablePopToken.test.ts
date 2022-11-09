import { mockPopToken } from '__tests__/utils';
import { OmitMethods } from 'core/types';

import { ScannablePopToken } from '../ScannablePopToken';

describe('ScannablePopToken object', () => {
  it('can build a defined object', () => {
    expect(
      () =>
        new ScannablePopToken({
          pop_token: mockPopToken.publicKey.toString(),
        }),
    ).not.toThrow(Error);
  });

  it('throws when pop_token is undefined', () => {
    expect(
      () =>
        new ScannablePopToken({
          pop_token: undefined,
        } as unknown as OmitMethods<ScannablePopToken>),
    ).toThrow(Error);
  });

  it('can encode in json', () => {
    const token = new ScannablePopToken({
      pop_token: mockPopToken.publicKey.toString(),
    });
    const expectedJson = '{"pop_token":"oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms="}';
    expect(token.toJson()).toEqual(expectedJson);
    expect(
      ScannablePopToken.encodePopToken({ pop_token: mockPopToken.publicKey.toString() }),
    ).toEqual(expectedJson);
  });
});
