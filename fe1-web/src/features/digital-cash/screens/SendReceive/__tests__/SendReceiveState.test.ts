import { mockPopToken } from '__tests__/utils';

import {
  SendReciveStateActionType,
  digitalCashWalletStateReducer as reduce,
} from '../SendReceiveState';

const emptyState = {
  showModal: false,
  beneficiaries: [{ amount: '', popToken: '' }],
  error: null,
};

const mockError = 'some error';

describe('SendReceiveState', () => {
  describe('reducer', () => {
    it('throws an exception for unkown actions', () => {
      expect(() => reduce(emptyState, { type: 'INVALID_ACTION' } as any)).toThrow(Error);
    });

    it('sets the error correctly', () => {
      const newState = reduce(emptyState, {
        type: SendReciveStateActionType.SET_ERROR,
        error: mockError,
      });

      expect(newState).toHaveProperty('error', mockError);
    });

    it('clears the error correctly', () => {
      const newState = reduce(
        { ...emptyState, error: mockError },
        {
          type: SendReciveStateActionType.CLEAR_ERROR,
        },
      );

      expect(newState).toHaveProperty('error', null);
    });

    it('sets the scanned pop token correctly', () => {
      const newState = reduce(
        {
          ...emptyState,
          beneficiaries: [
            { amount: '0', popToken: 'a' },
            { amount: '0', popToken: 'b' },
            { amount: '0', popToken: 'c' },
          ],
        },
        {
          type: SendReciveStateActionType.INSERT_SCANNED_POP_TOKEN,
          beneficiaryIndex: 2,
          beneficiaryPopToken: mockPopToken.publicKey.valueOf(),
        },
      );

      expect(newState).toHaveProperty('beneficiaries', [
        expect.anything(),
        expect.anything(),
        { amount: '0', popToken: mockPopToken.publicKey.valueOf() },
      ]);
    });

    it('updates the amount and popToken of beneficiaries correctly', () => {
      const newState = reduce(
        {
          ...emptyState,
          beneficiaries: [
            { amount: '', popToken: '' },
            { amount: '', popToken: '' },
            { amount: '', popToken: '' },
          ],
        },
        {
          type: SendReciveStateActionType.UPDATE_BENEFICIARY,
          beneficiaryIndex: 1,
          amount: '0',
          popToken: 'a',
        },
      );

      expect(newState.beneficiaries).toEqual([
        { amount: '', popToken: '' },
        { amount: '0', popToken: 'a' },
        { amount: '', popToken: '' },
      ]);
    });

    it('keeps the amount of beneficiaries if undefined is passed', () => {
      const newState = reduce(
        {
          ...emptyState,
          beneficiaries: [
            { amount: '', popToken: '' },
            { amount: 'some amount', popToken: '' },
            { amount: '', popToken: '' },
          ],
        },
        {
          type: SendReciveStateActionType.UPDATE_BENEFICIARY,
          beneficiaryIndex: 1,
          popToken: 'a',
        },
      );

      expect(newState.beneficiaries).toEqual([
        { amount: '', popToken: '' },
        { amount: 'some amount', popToken: 'a' },
        { amount: '', popToken: '' },
      ]);
    });

    it('keeps the pop token of beneficiaries if undefined is passed', () => {
      const newState = reduce(
        {
          ...emptyState,
          beneficiaries: [
            { amount: '', popToken: '' },
            { amount: '', popToken: 'some pop token' },
            { amount: '', popToken: '' },
          ],
        },
        {
          type: SendReciveStateActionType.UPDATE_BENEFICIARY,
          beneficiaryIndex: 1,
          amount: '42',
        },
      );

      expect(newState.beneficiaries).toEqual([
        { amount: '', popToken: '' },
        { amount: '42', popToken: 'some pop token' },
        { amount: '', popToken: '' },
      ]);
    });

    it('adds beneficiaries correctly', () => {
      const newState = reduce(
        {
          ...emptyState,
          beneficiaries: [
            { amount: '', popToken: '' },
            { amount: '', popToken: '' },
            { amount: '', popToken: '' },
          ],
        },
        {
          type: SendReciveStateActionType.ADD_BENEFICIARY,
        },
      );

      expect(newState.beneficiaries.length).toEqual(4);
    });
  });
});
