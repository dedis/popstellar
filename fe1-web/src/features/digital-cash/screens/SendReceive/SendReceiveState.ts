// This file handles the complex state of the 'SendReceive' screen

type SendReciveState = {
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

export enum SendReciveStateActionType {
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
   * Updates a given beneficiaries properties
   */
  UPDATE_BENEFICIARY = 'UPDATE_BENEFICIARY',

  /**
   * Adds a new (empty) beneficiary
   */
  ADD_BENEFICIARY = 'ADD_BENEFICIARY',
}

type SetError = {
  type: SendReciveStateActionType.SET_ERROR;
  error: string;
};

type ClearError = {
  type: SendReciveStateActionType.CLEAR_ERROR;
};

type InsertScannedPopToken = {
  type: SendReciveStateActionType.INSERT_SCANNED_POP_TOKEN;
  beneficiaryIndex: number;
  beneficiaryPopToken: string;
};

type UpdateBeneficiary = {
  type: SendReciveStateActionType.UPDATE_BENEFICIARY;
  beneficiaryIndex: number;
  amount?: string;
  popToken?: string;
};

type AddBeneficiary = {
  type: SendReciveStateActionType.ADD_BENEFICIARY;
};

export type DigitalCashWalletAction =
  | SetError
  | ClearError
  | InsertScannedPopToken
  | UpdateBeneficiary
  | AddBeneficiary;

/**
 * This is a react reducer, making state managment for complex states a bit
 * easier to read
 */
export const digitalCashWalletStateReducer = (
  state: SendReciveState,
  action: DigitalCashWalletAction,
): SendReciveState => {
  const { type } = action;
  switch (type) {
    case SendReciveStateActionType.SET_ERROR:
      return {
        ...state,
        error: action.error,
      };
    case SendReciveStateActionType.CLEAR_ERROR:
      return {
        ...state,
        error: null,
      };
    case SendReciveStateActionType.INSERT_SCANNED_POP_TOKEN: {
      const newBeneficiaries = [...state.beneficiaries];

      newBeneficiaries[action.beneficiaryIndex] = {
        amount: state.beneficiaries[action.beneficiaryIndex].amount,
        popToken: action.beneficiaryPopToken,
      };

      return {
        ...state,
        beneficiaries: newBeneficiaries,
      };
    }
    case SendReciveStateActionType.UPDATE_BENEFICIARY: {
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
    case SendReciveStateActionType.ADD_BENEFICIARY:
      return {
        ...state,
        beneficiaries: [...state.beneficiaries, { amount: '', popToken: '' }],
      };
    default:
      throw new Error(`Unkown action type '${type}'`);
  }
};
