import { WalletStore } from 'store';
import * as Seed from '../Seed';
import * as Token from '../Token';
import { Hash } from '../../Hash';
import { Base64UrlData } from '../../Base64Url';

jest.mock('platform/Storage');
jest.mock('platform/crypto/browser');

const mnemonic: string = 'garbage effort river orphan negative kind outside quit hat camera approve first';

test('Inputs produce known token - test vector 0', async () => {
  const expected = Base64UrlData.fromBuffer(Buffer.from(
    '7147759d146897111bcf74f60a1948b1d3a22c9199a6b88c236eb7326adc2efc', 'hex',
  ));

  await Seed.importMnemonic(mnemonic);

  const laoId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
  const rollCallId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
  const token = await Token.generateToken(laoId, rollCallId);

  expect(token.publicKey.valueOf()).toEqual(expected.valueOf());
});
