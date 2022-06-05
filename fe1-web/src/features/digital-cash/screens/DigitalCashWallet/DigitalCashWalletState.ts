import { RollCallAccount } from 'features/digital-cash/objects/Account';

// This file handles the complex state of the 'DigitalCashWallet' file

type DigitalCashWalletState = {
  /**
   * Whether to show the modal for sending & receiving money
   */
  showModal: boolean;

  /**
   * The selected account used to receive or send money.
   */
  selectedAccount: RollCallAccount | null;

  /**
   * The list of all beneficiaries that should receive a certain
   * amount of money
   */
  beneficiaries: { popToken: string; amount: string }[];

  /**
   * The error message to display before the send transaction button
   */
  error: string | null;
};

export enum DigitalCashWalletActionType {
  /**
   * Sets the error to be displayed.
   */
  SET_ERROR = 'SET_ERROR',

  /**
   * If an error was displayed before, this action will remove it
   */
  CLEAR_ERROR = 'CLEAR_ERROR',

  /**
   * Action to set the pop token of a given beneficiary after scanning it.
   * Also automatocally re-opens the model for the correct account
   */
  INSERT_SCANNED_POP_TOKEN = 'INSERT_SCANNED_POP_TOKEN',

  /**
   * Opens the receive & send modal for a given account
   */
  OPEN_MODAL = 'OPEN_MODAL',

  /**
   * Closes the receive & send modal
   */
  CLOSE_MODAL = 'CLOSE_MODAL',
  /**
   * HIDE_MODAL should only be used when it is *automatically* opened again later.
   * Otherwise use CLOSE_MODAL
   */
  HIDE_MODAL = 'HIDE_MODAL',

  /**
   * Updates a given beneficiaries properties
   */
  UPDATE_BENEFICIARY = 'UPDATE_BENEFICIARY',

  /**
   * Adds a new (empty) beneficiary
   */
  ADD_BENEFICIARY = 'ADD_BENEFICIARY',
}

type WalletSetError = {
  type: DigitalCashWalletActionType.SET_ERROR;
  error: string;
};

type WalletClearError = {
  type: DigitalCashWalletActionType.CLEAR_ERROR;
};

type WalletInsertScannedPopToken = {
  type: DigitalCashWalletActionType.INSERT_SCANNED_POP_TOKEN;
  account: RollCallAccount;
  beneficiaryIndex: number;
  beneficiaryPopToken: string;
};

type WalletOpenModal = {
  type: DigitalCashWalletActionType.OPEN_MODAL;
  account: RollCallAccount;
};

type WalletCloseModal = {
  type: DigitalCashWalletActionType.CLOSE_MODAL;
};

type WalletHideModal = {
  type: DigitalCashWalletActionType.HIDE_MODAL;
};

type WalletUpdateBeneficiary = {
  type: DigitalCashWalletActionType.UPDATE_BENEFICIARY;
  beneficiaryIndex: number;
  amount?: string;
  popToken?: string;
};

type WalletAddBeneficiary = {
  type: DigitalCashWalletActionType.ADD_BENEFICIARY;
};

export type DigitalCashWalletAction =
  | WalletSetError
  | WalletClearError
  | WalletInsertScannedPopToken
  | WalletOpenModal
  | WalletCloseModal
  | WalletHideModal
  | WalletUpdateBeneficiary
  | WalletAddBeneficiary;

/**
 * This is a react reducer, making state managment for complex states a bit
 * easier to read
 */
export const digitalCashWalletStateReducer = (
  state: DigitalCashWalletState,
  action: DigitalCashWalletAction,
): DigitalCashWalletState => {
  const { type } = action;
  switch (type) {
    case DigitalCashWalletActionType.SET_ERROR:
      return {
        ...state,
        error: action.error,
      };
    case DigitalCashWalletActionType.CLEAR_ERROR:
      return {
        ...state,
        error: null,
      };
    case DigitalCashWalletActionType.INSERT_SCANNED_POP_TOKEN: {
      const newBeneficiaries = [...state.beneficiaries];

      newBeneficiaries[action.beneficiaryIndex] = {
        amount: state.beneficiaries[action.beneficiaryIndex].amount,
        popToken: action.beneficiaryPopToken,
      };

      return {
        ...state,
        showModal: true,
        selectedAccount: action.account,
        beneficiaries: newBeneficiaries,
      };
    }
    case DigitalCashWalletActionType.OPEN_MODAL:
      return {
        ...state,
        showModal: true,
        selectedAccount: action.account,
      };
    case DigitalCashWalletActionType.CLOSE_MODAL:
      return {
        ...state,
        showModal: false,
        selectedAccount: null,
        beneficiaries: [{ amount: '', popToken: '' }],
      };
    case DigitalCashWalletActionType.HIDE_MODAL:
      return {
        ...state,
        showModal: false,
      };
    case DigitalCashWalletActionType.UPDATE_BENEFICIARY: {
      if (action.beneficiaryIndex >= state.beneficiaries.length) {
        throw new Error(
          `Cannot update inexistent benenficiary with index ${action.beneficiaryIndex}`,
        );
      }

      const newBeneficiaries = [...state.beneficiaries];

      newBeneficiaries[action.beneficiaryIndex] = {
        amount:
          action.amount !== undefined
            ? action.amount
            : state.beneficiaries[action.beneficiaryIndex].amount,
        popToken:
          action.popToken !== undefined
            ? action.popToken
            : state.beneficiaries[action.beneficiaryIndex].popToken,
      };

      return { ...state, beneficiaries: newBeneficiaries };
    }
    case DigitalCashWalletActionType.ADD_BENEFICIARY:
      return {
        ...state,
        beneficiaries: [...state.beneficiaries, { amount: '', popToken: '' }],
      };
    default:
      throw new Error(`Unkown action type '${type}'`);
  }
};
