import { PublicKey } from 'core/objects';

import { RollCallToken } from '../objects/RollCallToken';

export function requestSendTransaction(from: RollCallToken, to: PublicKey, value: number) {
  // 1. Find all transactions with the "from" public key (hash) in their txOut
  // 2. Compute the total value of all these txOuts and check that it is bigger than value
  // 3. Create a new transaction with value sent to "to" and the rest of the balance to "from"
}
