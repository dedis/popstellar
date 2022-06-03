import { mockCBHash, mockValidCoinbaseJSON } from '__tests__/utils';
import { Hash, ProtocolError } from 'core/objects';

import { PostTransaction } from '../PostTransaction';

const postTransactionObject = {
  transaction: mockValidCoinbaseJSON,
  transaction_id: new Hash(mockCBHash),
};

describe('PostTransaction', () => {
  it('should be created correctly from Json', () => {
    const postTransaction = new PostTransaction(postTransactionObject);
    expect(postTransaction).toBeDefined();
    expect(postTransaction.transaction_id).toEqual(postTransactionObject.transaction_id);
    expect(postTransaction.transaction).toEqual(postTransaction.transaction);
  });
  it('should be created correctly from object', () => {
    const postTransaction = PostTransaction.fromJSON(postTransactionObject);
    expect(postTransaction).toBeDefined();
    expect(postTransaction.transaction_id).toEqual(postTransactionObject.transaction_id);
    expect(postTransaction.transaction).toEqual(postTransaction.transaction);
  });
  it('should fail to create a message when undefined transaction', () => {
    const object = {
      transaction_id: postTransactionObject.transaction_id,
    };
    expect(() => new PostTransaction(object)).toThrow(ProtocolError);
  });
  it('should fail to create a message when undefined transaction_id', () => {
    const object = {
      transaction: postTransactionObject.transaction,
    };
    expect(() => new PostTransaction(object)).toThrow(ProtocolError);
  });
});
