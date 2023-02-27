import { useContext, useMemo } from 'react';
import { useSelector } from 'react-redux';

import FeatureContext from 'core/contexts/FeatureContext';
import { Hash, RollCallToken } from 'core/objects';

import { DigitalCashReactContext, DIGITAL_CASH_FEATURE_IDENTIFIER } from '../interface';
import { Transaction } from '../objects/transaction';
import {
  makeBalancesSelector,
  makeTransactionsByHashSelector,
  makeTransactionsByRollCallTokenSelector,
  makeTransactionsSelector,
} from '../reducer';

export namespace DigitalCashHooks {
  export const useDigitalCashContext = (): DigitalCashReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the digital cash context exists
    if (!(DIGITAL_CASH_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Digital cash context could not be found!');
    }
    return featureContext[DIGITAL_CASH_FEATURE_IDENTIFIER] as DigitalCashReactContext;
  };

  /**
   * Gets the current lao id, throws an error if there is none
   */
  export const useCurrentLaoId = () => useDigitalCashContext().useCurrentLaoId();

  /**
   * Returns true if currently connected to a lao, false if in offline mode
   * and undefined if there is no current lao
   */
  export const useConnectedToLao = () => useDigitalCashContext().useConnectedToLao();

  /**
   * Gets whether the current user is organizer of the given lao
   */
  export const useIsLaoOrganizer = (laoId: Hash) =>
    useDigitalCashContext().useIsLaoOrganizer(laoId);

  /**
   * Gets the roll call tokens for a given lao id
   */
  export const useRollCallTokensByLaoId = (laoId: Hash) =>
    useDigitalCashContext().useRollCallTokensByLaoId(laoId);

  /**
   * Gets the roll call token for a given lao id and a given roll call id
   */
  export const useRollCallTokenByRollCallId = (laoId: Hash, rollCallId?: Hash) =>
    useDigitalCashContext().useRollCallTokenByRollCallId(laoId, rollCallId);

  /**
   * Gets the roll call for a given id
   */
  export const useRollCallById = (rollCallId?: Hash) =>
    useDigitalCashContext().useRollCallById(rollCallId);

  /**
   * Gets all roll calls for a given lao id
   */
  export const useRollCallsByLaoId = (laoId: Hash) =>
    useDigitalCashContext().useRollCallsByLaoId(laoId);

  /**
   * Gets the list of all transactions that happened in this LAO
   * To use only in a React component
   * @param laoId the id of the LAO
   */
  export const useTransactions = (laoId: Hash) => {
    const transactionsSelector = useMemo(() => makeTransactionsSelector(laoId), [laoId]);

    const transactionStates = useSelector(transactionsSelector);

    return useMemo(
      () => transactionStates.map((state) => Transaction.fromState(state)),
      [transactionStates],
    );
  };

  /**
   * Gets the mapping between transaction hashes and the transaction states in a lao
   * To use only in a react component
   * @param laoId the id of the LAO
   */
  export const useTransactionsByHash = (laoId: Hash) => {
    const transactionsByHashSelector = useMemo(
      () => makeTransactionsByHashSelector(laoId),
      [laoId],
    );
    return useSelector(transactionsByHashSelector);
  };

  /**
   * Gets the total balance of all roll call tokens belonging to the current user and its seed
   * To use only in a React component
   * @param laoId
   */
  export const useTotalBalance = (laoId: Hash) => {
    const rollCallTokens = useRollCallTokensByLaoId(laoId);

    const balancesSelector = useMemo(() => makeBalancesSelector(laoId), [laoId]);
    const balances = useSelector(balancesSelector);

    return useMemo(
      () =>
        rollCallTokens.reduce(
          (sum, rollCallToken) =>
            sum + (balances[Hash.fromPublicKey(rollCallToken.token.publicKey).valueOf()] || 0),
          0,
        ),
      [rollCallTokens, balances],
    );
  };

  /**
   * Gets the list of all transactions where a list of roll call tokens are involved.
   * To use only in a React component.
   * @param laoId
   * @param rollCallTokens
   */
  export const useTransactionsByRollCallTokens = (laoId: Hash, rollCallTokens: RollCallToken[]) => {
    const transactionsByTokensSelector = useMemo(
      () => makeTransactionsByRollCallTokenSelector(laoId, rollCallTokens),
      [laoId, rollCallTokens],
    );

    const transactionStates = useSelector(transactionsByTokensSelector);

    return useMemo(
      () => transactionStates.map((state) => Transaction.fromState(state)),
      [transactionStates],
    );
  };
}
