import { mockPopToken, mockPublicKey } from '__tests__/utils';
import { mockRollCall } from 'features/rollCall/__tests__/utils';

import {
  DigitalCashWalletActionType,
  digitalCashWalletStateReducer as reduce,
} from '../DigitalCashWalletState';

const emptyState = {
  showModal: false,
  selectedAccount: null,
  beneficiaries: [{ amount: '', popToken: '' }],
  error: null,
};

const mockError = 'some error';

const mockAccount = {
  balance: 0,
  popToken: mockPublicKey.valueOf(),
  rollCallId: mockRollCall.id.valueOf(),
  rollCallName: mockRollCall.name,
};

describe('DigitalCashWalletState', () => {
  describe('reducer', () => {
    it('throws an exception for unkown actions', () => {
      expect(() => reduce(emptyState, { type: 'INVALID_ACTION' } as any)).toThrow(Error);
    });

    it('sets the error correctly', () => {
      const newState = reduce(emptyState, {
        type: DigitalCashWalletActionType.SET_ERROR,
        error: mockError,
      });

      expect(newState).toHaveProperty('error', mockError);
    });

    it('clears the error correctly', () => {
      const newState = reduce(
        { ...emptyState, error: mockError },
        {
          type: DigitalCashWalletActionType.CLEAR_ERROR,
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
          type: DigitalCashWalletActionType.INSERT_SCANNED_POP_TOKEN,
          account: mockAccount,
          beneficiaryIndex: 2,
          beneficiaryPopToken: mockPopToken.publicKey.valueOf(),
        },
      );

      expect(newState).toHaveProperty('selectedAccount', mockAccount);
      expect(newState).toHaveProperty('beneficiaries', [
        expect.anything(),
        expect.anything(),
        { amount: '0', popToken: mockPopToken.publicKey.valueOf() },
      ]);
    });

    it('opens the modal correctly', () => {
      const newState = reduce(emptyState, {
        type: DigitalCashWalletActionType.OPEN_MODAL,
        account: mockAccount,
      });

      expect(newState).toHaveProperty('showModal', true);
      expect(newState).toHaveProperty('selectedAccount', mockAccount);
    });

    it('closes the modal correctly', () => {
      const newState = reduce(
        {
          ...emptyState,
          showModal: true,
          selectedAccount: mockAccount,
          beneficiaries: [
            { amount: '0', popToken: 'a' },
            { amount: '0', popToken: 'b' },
          ],
        },
        {
          type: DigitalCashWalletActionType.CLOSE_MODAL,
        },
      );

      expect(newState).toHaveProperty('showModal', false);
      expect(newState).toHaveProperty('selectedAccount', null);
      expect(newState).toHaveProperty('beneficiaries', [{ amount: '', popToken: '' }]);
    });

    it('hides the modal correctly', () => {
      const newState = reduce(
        {
          ...emptyState,
          showModal: true,
          selectedAccount: mockAccount,
        },
        {
          type: DigitalCashWalletActionType.HIDE_MODAL,
        },
      );

      expect(newState).toHaveProperty('showModal', false);
      // this time the selected account should *not* change
      expect(newState).toHaveProperty('selectedAccount', mockAccount);
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
          type: DigitalCashWalletActionType.UPDATE_BENEFICIARY,
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
          type: DigitalCashWalletActionType.UPDATE_BENEFICIARY,
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
          type: DigitalCashWalletActionType.UPDATE_BENEFICIARY,
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
          type: DigitalCashWalletActionType.ADD_BENEFICIARY,
        },
      );

      expect(newState.beneficiaries.length).toEqual(4);
    });
  });
});
