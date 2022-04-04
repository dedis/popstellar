import { expect } from 'chai';

import { mockLao, mockRC } from '__tests__/utils';
import { PopToken } from 'core/objects';

import { RollCallToken } from '../RollCallToken';

const mockKP = {
  publicKey: '0x000000123',
  privateKey: '0x182546789',
};
const mockToken = PopToken.fromState(mockKP);
const rctState = {
  token: mockKP,
  laoId: mockLao.id.valueOf(),
  rollCallId: mockRC.id.valueOf(),
  rollCallName: mockRC.name,
};

describe('Roll call token object', () => {
  it('should be able to create a token', () => {
    const rctP = new RollCallToken({
      token: mockToken,
      laoId: mockLao.id,
      rollCallId: mockRC.id,
      rollCallName: mockRC.name,
    });
    const rctS = RollCallToken.fromState(rctState);
    expect(rctP).to.be.eql(rctS);
  });
  it('should be able to convert object to a correct state', () => {
    const rct = RollCallToken.fromState(rctState);
    expect(rct.toState()).to.be.eql(rctState);
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
