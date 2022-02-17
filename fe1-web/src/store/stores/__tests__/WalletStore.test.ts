import 'jest-extended';
import * as store from 'store/Storage';
import { WalletStore } from '../WalletStore';

jest.mock('platform/Storage');
jest.mock('platform/crypto/browser');
jest.mock('store/Storage');

const seed = new Uint8Array([0x1, 0x8, 0xF, 0x22, 0xFF, 0x33, 0x99]);
const mnemonic = 'probably insecure definitely memorable';

test('should report no seed at startup', () => {
  expect(WalletStore.hasSeed()).toBeFalse();
});

test('should report a seed after initialization', async () => {
  await WalletStore.store(mnemonic, seed);
  expect(WalletStore.hasSeed()).toBeTrue();
});

test('should not store the mnemonic in cleartext', async () => {
  await WalletStore.store(mnemonic, seed);
  const walletState = store.getStore().getState().wallet;
  expect(walletState.mnemonic).not.toEqual(mnemonic);
});

test('should not store the seed in cleartext', async () => {
  await WalletStore.store(mnemonic, seed);
  const walletState = store.getStore().getState().wallet;
  expect(walletState.seed).not.toEqual(seed.toString());
});

test('should correctly encrypt and decrypt the mnemonic', async () => {
  await WalletStore.store(mnemonic, seed);
  const actual = await WalletStore.getMnemonic();
  expect(actual).toEqual(mnemonic);
});

test('should correctly encrypt and decrypt the seed', async () => {
  await WalletStore.store(mnemonic, seed);
  const actual = await WalletStore.getSeed();
  expect(actual).toEqual(seed);
});
