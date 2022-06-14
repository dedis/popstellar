import { useContext, useMemo } from 'react';
import { useSelector } from 'react-redux';

import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';

import { DigitalCashReactContext, DIGITAL_CASH_FEATURE_IDENTIFIER } from '../interface';
import { Transaction } from '../objects/transaction';
import {
  makeBalancesSelector,
  makeTransactionsByHashSelector,
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
   * Gets the current lao id
   */
  export const useCurrentLaoId = () => useDigitalCashContext().useCurrentLaoId();

  /**
   * Gets whether the current user is organizer of the given lao
   */
  export const useIsLaoOrganizer = (laoId: string) =>
    useDigitalCashContext().useIsLaoOrganizer(laoId);

  /**
   * Gets the roll call tokens for a given lao id
   */
  export const useRollCallTokensByLaoId = (laoId: string) =>
    useDigitalCashContext().useRollCallTokensByLaoId(laoId);

  /**
   * Gets the roll call token for a given lao id and a given roll call id
   */
  export const useRollCallTokenByRollCallId = (laoId: string, rollCallId: string) =>
    useDigitalCashContext().useRollCallTokenByRollCallId(laoId, rollCallId);

  /**
   * Gets the roll call for a given id
   */
  export const useRollCallById = (rollCallId: Hash | string) =>
    useDigitalCashContext().useRollCallById(rollCallId);

  /**
   * Gets all roll calls for a given lao id
   */
  export const useRollCallsByLaoId = (laoId: string) =>
    useDigitalCashContext().useRollCallsByLaoId(laoId);

  /**
   * Gets the list of all transactions that happened in this LAO
   * To use only in a React component
   * @param laoId the id of the LAO
   */
  export const useTransactions = (laoId: Hash | string) => {
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
  export const useTransactionsByHash = (laoId: Hash | string) => {
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
  export const useTotalBalance = (laoId: string | Hash) => {
    const rollCallTokens = useRollCallTokensByLaoId(laoId.valueOf());

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
}
