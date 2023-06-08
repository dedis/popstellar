import { mockPopToken } from '__tests__/utils';
import { OmitMethods } from 'core/types';

import { ProtocolError } from '../ProtocolError';
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

  it('throws if the pop_token is not in a valid format', () => {
    const validPopTokens = [
      'QRf_GkuQI6-TviY6ZmW3sMZQzDYc66SWqcgfgKyiY00=',
      '-r4fYiL7-uAnt4WIQ2-XopjzwCVBKyTECNr420juktI=',
      'abcdefghijklwxyzABNOPQRSTUVWXYZ0123456789-_=',
    ];

    const invalidPopTokens = [
      '', // empty string
      'ockPublicKey2_fFcHDaVHcCcY8IBfHE7auXJ7h4ms=', // size not multiple of 44
      'mockP!blicKey2_fFcHDaVHcCcY8IBfHE7auXJ7h4ms=', // invalid character
    ];

    validPopTokens.forEach((popToken) => {
      expect(
        () =>
          new ScannablePopToken({
            pop_token: popToken,
          }),
      ).not.toThrow(Error);
    });

    invalidPopTokens.forEach((popToken) => {
      expect(
        () =>
          new ScannablePopToken({
            pop_token: popToken,
          }),
      ).toThrow(ProtocolError);
    });
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
