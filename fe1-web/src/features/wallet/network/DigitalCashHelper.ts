import { Hash } from 'core/objects';

import { DigitalCashTransaction } from './DigitalCashTransaction';

export const hashTransaction = (transaction: DigitalCashTransaction) => {
  return Hash.fromString(JSON.stringify(transaction));
};
