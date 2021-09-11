import { WalletStore } from 'store';
import * as Wallet from '../wallet';
import * as Bip39Path from '../wallet/Bip39Path';
import { Hash } from '../Hash';
import { Base64UrlData } from '../Base64Url';

jest.mock('platform/Storage');
jest.mock('platform/crypto/browser');

const mnemonic: string = 'garbage effort river orphan negative kind outside quit hat camera approve first';

test('Known mnemonic produces known seed', async () => {
  const expected: string = '010ac98c615c31a20a6a9fcb71c94642abdd4f662d148f81d61479c8f125854bac9c0228f6705cbdd96e27ffb2d4e806d152c875a5484113434d1d561e42a94d';

  await Wallet.importMnemonic(mnemonic);
  const seedArray = await WalletStore.getSeed();
  const seedHex = Buffer.from(seedArray).toString('hex');

  expect(seedHex).toBe(expected);
});

test('Known inputs should produce same path', async () => {

  const laoId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
  const rollCallId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');

  const expected = [
    "m",
    "888'",
    "0'",
    "7920043'/38174203'/71210134'/14078251'/2278823'/50163231'/203204108'/4625150'/6448'/23105'/238184'",
    "7920043'/38174203'/71210134'/14078251'/2278823'/50163231'/203204108'/4625150'/6448'/23105'/238184'"
  ].join('/');

  expect(Bip39Path.fromLaoRollCall(laoId, rollCallId))
    .toEqual(expected);
});

test('Known path should produce same token', async () => {
  const expected = Base64UrlData.fromBuffer(Buffer.from(
    '7147759d146897111bcf74f60a1948b1d3a22c9199a6b88c236eb7326adc2efc', 'hex'));

  const path = [
    "m",
    "888'",
    "0'",
    "7920043'/38174203'/71210134'/14078251'/2278823'/50163231'/203204108'/4625150'/6448'/23105'/238184'",
    "7920043'/38174203'/71210134'/14078251'/2278823'/50163231'/203204108'/4625150'/6448'/23105'/238184'"
  ].join('/');

  await Wallet.importMnemonic(mnemonic);

  const token = await Wallet.generateTokenFromPath(path);
  expect(token.publicKey.valueOf()).toEqual(expected.valueOf());
})

test('Known inputs should produce same token', async () => {
  const expected = Base64UrlData.fromBuffer(Buffer.from(
    '7147759d146897111bcf74f60a1948b1d3a22c9199a6b88c236eb7326adc2efc', 'hex'));

  await Wallet.importMnemonic(mnemonic);

  const laoId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
  const rollCallId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
  const token = await Wallet.generateToken(laoId, rollCallId);

  expect(token.publicKey.valueOf()).toEqual(expected.valueOf());
})
