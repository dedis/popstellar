import { configureTestFeatures } from '__tests__/utils';

import { WalletStore } from '../../store';
import * as Seed from '../Seed';

jest.mock('core/platform/Storage');
jest.mock('core/platform/crypto/browser');

const mnemonic: string =
  'garbage effort river orphan negative kind outside quit hat camera approve first';

beforeAll(configureTestFeatures);

beforeEach(() => {
  WalletStore.clear();
});

test('Mnemonic produces known seed - test vector 0', async () => {
  const expected: string =
    '010ac98c615c31a20a6a9fcb71c94642abdd4f662d148f81d61479c8f125854bac9c0228f6705cbdd96e27ffb2d4e806d152c875a5484113434d1d561e42a94d';

  await Seed.importMnemonic(mnemonic);
  const seedArray = await WalletStore.getSeed();
  const seedHex = Buffer.from(seedArray).toString('hex');

  expect(seedHex).toBe(expected);
});

test("Mnemonic can't be extracted before importing", async () => {
  await expect(async () => {
    await Seed.exportMnemonic();
  }).rejects.toThrow();
});

test('Mnemonic can be extracted from store', async () => {
  await Seed.importMnemonic(mnemonic);

  const actual = await Seed.exportMnemonic();

  expect(actual).toBe(mnemonic);
});

test('Mnemonic can be forgotten', async () => {
  await Seed.importMnemonic(mnemonic);
  Seed.forget();
  await expect(async () => {
    await Seed.exportMnemonic();
  }).rejects.toThrow();
});
