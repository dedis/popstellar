import { Base64UrlData, Hash } from 'core/objects';

import { WalletStore } from '../../store';
import * as Seed from '../Seed';
import * as Wallet from '../index';
import * as Token from '../Token';

jest.mock('core/platform/Storage');
jest.mock('core/platform/crypto/browser');
const mockId = 'T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=';

const mnemonic: string =
  'garbage effort river orphan negative kind outside quit hat camera approve first';

beforeEach(() => {
  WalletStore.clear();
});

test('LAO/RollCall produce known token - test vector 0', async () => {
  const expected = Base64UrlData.fromBuffer(
    Buffer.from('7147759d146897111bcf74f60a1948b1d3a22c9199a6b88c236eb7326adc2efc', 'hex'),
  );

  await Seed.importMnemonic(mnemonic);

  const laoId: Hash = new Hash(mockId);
  const rollCallId: Hash = new Hash(mockId);
  const token = await Token.generateToken(laoId, rollCallId);

  expect(token!!.publicKey.valueOf()).toEqual(expected.valueOf());
});

test('LAO/RollCall produces correct signature 1', async () => {
  await Seed.importMnemonic(mnemonic);

  const laoId: Hash = new Hash(mockId);
  const rollCallId: Hash = new Hash(mockId);
  const token = await Token.generateToken(laoId, rollCallId);

  // sign some data with token
  const data = Base64UrlData.encode('this is my super secure data');
  const signature = token!!.privateKey.sign(data);
  // verify signature with token public key
  expect(signature.verify(token!!.publicKey, data)).toBeTrue();
});

test('LAO/RollCall produces correct signature 2', async () => {
  await Seed.importMnemonic(mnemonic);

  const laoId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
  const rollCallId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
  const token = await Token.generateToken(laoId, rollCallId);

  // sign some data with token
  const data = Base64UrlData.encode('this is my super secure data');
  const signature = token!.privateKey.sign(data);
  // verify signature with token public key
  expect(signature.verify(token!.publicKey, data)).toBeTrue();
});

test('Path produces known token - test vector 0', async () => {
  const expected = Base64UrlData.fromBuffer(
    Buffer.from('7147759d146897111bcf74f60a1948b1d3a22c9199a6b88c236eb7326adc2efc', 'hex'),
  );

  await Seed.importMnemonic(mnemonic);

  const path = [
    'm',
    "888'",
    "0'",
    "7920043'/38174203'/71210134'/14078251'/2278823'/50163231'/203204108'/4625150'/6448'/23105'/238184'",
    "7920043'/38174203'/71210134'/14078251'/2278823'/50163231'/203204108'/4625150'/6448'/23105'/238184'",
  ].join('/');

  await Wallet.importMnemonic(mnemonic);

  const token = await Token.generateTokenFromPath(path);
  expect(token.publicKey.valueOf()).toEqual(expected.valueOf());
});

test('generateToken returns undefined with an undefined Roll call id', async () => {
  const laoId = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
  const token = await Token.generateToken(laoId, undefined);

  expect(token).toEqual(undefined);
});
