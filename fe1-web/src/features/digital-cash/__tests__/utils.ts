import { mockKeyPair, mockLaoId } from '__tests__/utils';
import { Hash, PopToken, RollCallToken } from 'core/objects';
import { COINBASE_HASH, SCRIPT_TYPE } from 'resources/const';

import { mockRollCall as mockRC } from '../../rollCall/__tests__/utils';
import { DIGITAL_CASH_FEATURE_IDENTIFIER, DigitalCashReactContext } from '../interface';
import { TransactionJSON, TransactionState } from '../objects/transaction';

export const mockRollCall = mockRC;

export const mockRollCallToken = new RollCallToken({
  laoId: mockLaoId,
  rollCallName: mockRollCall.name,
  rollCallId: mockRollCall.id,
  token: PopToken.fromState(mockKeyPair.toState()),
});

export const mockDigitalCashContextValue = (isOrganizer: boolean) => ({
  [DIGITAL_CASH_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
    useIsLaoOrganizer: () => isOrganizer,
    useRollCallById: () => mockRollCall,
    useRollCallsByLaoId: () => ({
      [mockRollCall.id.valueOf()]: mockRollCall,
    }),
    useRollCallTokensByLaoId: () => [mockRollCallToken],
    useRollCallTokenByRollCallId: () => mockRollCallToken,
  } as DigitalCashReactContext,
});

// MOCK Transactions

export const mockTransactionValue = 100;
export const mockKPHash = Hash.fromPublicKey(mockKeyPair.publicKey);
export const mockCBSig =
  'ts5vHgbiGPu55Acj1Mo72kypWYfMZCs6eo4kXvCyf2UmVGmKfJXvm1JTS4o6Lk1wIDK-RepcUSHPkZHFT6jCDw==';
export const mockCBHash = new Hash('ZmVFcfCTuGi5YYpGTB_xzeYC6SfP_ernBOsakP-iq64=');
export const mockCoinbaseTransactionJSON: TransactionJSON = {
  version: 1,
  inputs: [
    {
      tx_out_hash: COINBASE_HASH,
      tx_out_index: 0,
      script: {
        type: SCRIPT_TYPE,
        pubkey: mockKeyPair.publicKey.valueOf(),
        sig: mockCBSig,
      },
    },
  ],
  outputs: [
    {
      value: mockTransactionValue,
      script: {
        type: SCRIPT_TYPE,
        pubkey_hash: Hash.fromPublicKey(mockKeyPair.publicKey).valueOf(),
      },
    },
  ],
  lock_time: 0,
};
export const mockTransactionHash = 'Sl_DPZl-qlXhjkudiZoSG9VscKU_cxm6AwBZzBEMK4M=';
export const mockTransactionState: TransactionState = {
  version: 1,
  inputs: [
    {
      txOutHash: mockCBHash.valueOf(),
      txOutIndex: 0,
      script: {
        type: SCRIPT_TYPE,
        publicKey: mockKeyPair.publicKey.valueOf(),
        signature:
          'cvIw1mKe52lJz5XRWIRTRWh-ztMcOJY6pYA9_GKmIQZB0c_1qu4hWlmD5VSft4gT1quSRptS5NOKbF6KDZjACw==',
      },
    },
  ],
  outputs: [
    {
      value: 100,
      script: {
        type: SCRIPT_TYPE,
        publicKeyHash: Hash.fromPublicKey(mockKeyPair.publicKey).valueOf(),
      },
    },
  ],
  transactionId: mockTransactionHash,
  lockTime: 0,
};
