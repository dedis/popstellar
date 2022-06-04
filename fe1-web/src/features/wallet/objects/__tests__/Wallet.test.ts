import { mockLao, configureTestFeatures } from '__tests__/utils';
import { Hash } from 'core/objects';
import { mockRollCallState } from 'features/rollCall/__tests__/utils';
import { RollCall } from 'features/rollCall/objects';

import { RollCallToken } from '../RollCallToken';
import * as Seed from '../Seed';
import { generateToken } from '../Token';
import { recoverWalletRollCallTokens } from '../Wallet';

const mnemonic: string =
  'garbage effort river orphan negative kind outside quit hat camera approve first';
const createMockRCWithAttendee = (publicKey: string) => {
  return RollCall.fromState({
    ...mockRollCallState,
    attendees: [publicKey],
  });
};

jest.mock('core/platform/Storage');
jest.mock('core/platform/crypto/browser');

beforeAll(async () => {
  configureTestFeatures();
  await Seed.importMnemonic(mnemonic);
});
describe('Recover wallet roll call tokens function', () => {
  it('Should be able to recover an existing roll call token', async () => {
    const popToken = await generateToken(mockLao.id, new Hash(mockRollCallState.id));
    const mockRC = createMockRCWithAttendee(popToken.publicKey.valueOf());

    const expected = new RollCallToken({
      token: popToken,
      laoId: mockLao.id,
      rollCallId: mockRC.id,
      rollCallName: mockRC.name,
    });

    const state = { [mockLao.id.valueOf()]: { [mockRC.id.valueOf()]: mockRC } };
    const rct = await recoverWalletRollCallTokens(state, mockLao.id);
    expect(rct).toEqual([expected]);
  });

  it('Should return an empty array when no corresponding token', async () => {
    const mockRC = createMockRCWithAttendee(mockRollCallState.attendees[0]);

    const state = { [mockLao.id.valueOf()]: { [mockRC.id.valueOf()]: mockRC } };
    const rct = await recoverWalletRollCallTokens(state, mockLao.id);
    expect(rct).toEqual([]);
  });
});
