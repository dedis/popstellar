import { useContext, useMemo } from 'react';
import { useSelector } from 'react-redux';

import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';

import { DigitalCashReactContext, DIGITAL_CASH_FEATURE_IDENTIFIER } from '../interface';
import { Transaction } from '../objects/transaction';
import { makeTransactionsSelector } from '../reducer';

export namespace DigitalCashHooks {
  export const useDigitalCashContext = (): DigitalCashReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the evoting context exists
    if (!(DIGITAL_CASH_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Events context could not be found!');
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
   * @param laoId the id of the LAO
   */
  export const useTransactions = (laoId: Hash | string) => {
    const transactionStates = useSelector(useMemo(() => makeTransactionsSelector(laoId), [laoId]));
    return useMemo(
      () => transactionStates.map((state) => Transaction.fromState(state)),
      [transactionStates],
    );
  };
}
