import * as Bip39Path from '../Bip32Path';
import { Hash } from '../../Hash';

test('Bip39Path produces the known outputs - test vector 0', async () => {
  const laoId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
  const rollCallId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');

  const expected = [
    'm',
    "888'",
    "0'",
    "7920043'/38174203'/71210134'/14078251'/2278823'/50163231'/203204108'/4625150'/6448'/23105'/238184'",
    "7920043'/38174203'/71210134'/14078251'/2278823'/50163231'/203204108'/4625150'/6448'/23105'/238184'",
  ].join('/');

  expect(Bip39Path.fromLaoRollCall(laoId, rollCallId)).toEqual(expected);
});

test('Bip39Path produces the known outputs - test vector 1', async () => {
  const laoId: Hash = new Hash('V6GxtAAe8TdkVl3bh6J6FvuixxPvjU_gScuY5uxohmk=');
  const rollCallId: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');

  const expected = [
    'm',
    "888'",
    "0'",
    "87161177'/180030'/24155100'/8693219'/135162122'/22251162'/19919239'/14179224'/73203152'/230236104'/134105'",
    "7920043'/38174203'/71210134'/14078251'/2278823'/50163231'/203204108'/4625150'/6448'/23105'/238184'",
  ].join('/');

  expect(Bip39Path.fromLaoRollCall(laoId, rollCallId)).toEqual(expected);
});
