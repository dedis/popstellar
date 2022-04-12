import { expect } from 'chai';

import { mockLao, mockRC } from '__tests__/utils';
import { PopToken } from 'core/objects';

import { RollCallToken } from '../RollCallToken';

const mockKP = {
  publicKey: '0x000000123',
  privateKey: '0x182546789',
};
const mockToken = PopToken.fromState(mockKP);

describe('Roll call token object', () => {
  it('should be able to create a token', () => {
    expect(
      () =>
        new RollCallToken({
          token: mockToken,
          laoId: mockLao.id,
          rollCallId: mockRC.id,
          rollCallName: mockRC.name,
        }),
    ).to.not.throw();
  });
  it('should fail when undefined fields', () => {
    expect(() => new RollCallToken({ token: undefined })).to.throw();
    expect(() => new RollCallToken({ token: mockToken })).to.throw();
    expect(() => new RollCallToken({ token: mockToken, laoId: mockLao.id })).to.throw();
    expect(
      () => new RollCallToken({ token: mockToken, laoId: mockLao.id, rollCallId: mockRC.id }),
    ).to.throw();
  });
});
