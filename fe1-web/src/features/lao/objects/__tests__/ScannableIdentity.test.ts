import { mockKeyPair } from '__tests__/utils';
import { OmitMethods } from 'core/types';

import { ScannableIdentity } from '../ScannableIdentity';

describe('ScannableIdentity object', () => {
  it('can build a defined object', () => {
    expect(
      () =>
        new ScannableIdentity({
          main_public_key: mockKeyPair.publicKey.toString(),
        }),
    ).not.toThrow(Error);
  });

  it('throws when pop_token is undefined', () => {
    expect(
      () =>
        new ScannableIdentity({
          pop_token: undefined,
        } as unknown as OmitMethods<ScannableIdentity>),
    ).toThrow(Error);
  });

  it('can encode in json', () => {
    const token = new ScannableIdentity({
      main_public_key: mockKeyPair.publicKey.toString(),
    });
    const expectedJson = '{"main_public_key":"J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="}';
    expect(token.toJson()).toEqual(expectedJson);
    expect(
      ScannableIdentity.encodeIdentity({ main_public_key: mockKeyPair.publicKey.toString() }),
    ).toEqual(expectedJson);
  });
});
