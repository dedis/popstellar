import { useContext, useMemo } from 'react';
import { useSelector } from 'react-redux';

import FeatureContext from 'core/contexts/FeatureContext';

import { RollCallFeature, RollCallReactContext, ROLLCALL_FEATURE_IDENTIFIER } from '../interface';
import { RollCall } from '../objects';
import { makeRollCallByIdSelector } from '../reducer';

export namespace RollCallHooks {
  export const useRollCallContext = (): RollCallReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the rollcall context exists
    if (!(ROLLCALL_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('RollCall context could not be found!');
    }
    return featureContext[ROLLCALL_FEATURE_IDENTIFIER] as RollCallReactContext;
  };

  /**
   * Gets the current lao id
   * @returns The current lao id
   */
  export const useCurrentLaoId = () => {
    const laoId = useRollCallContext().useCurrentLaoId();

    if (!laoId) {
      throw new Error('Error encountered while obtaining current lao id: no active LAO');
    }

    return laoId;
  };

  /**
   * Gets the generateToken function
   */
  export const useGenerateToken = () => useRollCallContext().generateToken;

  /**
   * Gets the hasSeed function
   */
  export const useHasSeed = () => useRollCallContext().hasSeed;

  /**
   * Creates a subset of rollCallById where the returned map only contains the keys in rollCallIds
   * @param rollCallIds The set of roll call ids that should be mapped to the corresponding roll call
   * @param rollCallById A map where all mappings from roll call id to roll call instance are contained
   */
  const getRollCallsId = (
    rollCallIds: string[],
    rollCallById: Record<string, RollCall>,
  ): {
    [rollCallId: string]: RollCall;
  } => {
    return rollCallIds.reduce((rollCallByIdMap, rollCallId) => {
      // in order to make the reduce() efficient, we want to reuse the object and thus
      // re-assign properties here
      // eslint-disable-next-line no-param-reassign
      rollCallByIdMap[rollCallId] = rollCallById[rollCallId];
      return rollCallByIdMap;
    }, {} as Record<string, RollCall>);
  };

  /**
   * Creates a map from laoId to maps from roll call id to roll call instances
   * @param rollCallStatesByLaoId A map from laoId to a map of roll call ids to roll call states
   * @param rollCallById A map from roll call id to roll call instance
   */
  const getRollCallsByLaoId = (
    rollCallStatesByLaoId: Record<string, Record<string, RollCallFeature.EventState>>,
    rollCallById: Record<string, RollCall>,
  ): {
    [laoId: string]: { [rollCallId: string]: RollCall };
  } => {
    // iterate over all lao ids
    return Object.keys(rollCallStatesByLaoId).reduce((rollCallsByLaoId, laoId) => {
      // for each lao id create a map from roll call id to a roll call instance

      // in order to make the reduce() efficient, we want to reuse the object and thus
      // re-assign properties here
      // eslint-disable-next-line no-param-reassign
      rollCallsByLaoId[laoId] = getRollCallsId(
        Object.keys(rollCallStatesByLaoId[laoId]),
        rollCallById,
      );

      return rollCallsByLaoId;
    }, {} as Record<string, Record<string, RollCall>>);
  };

  export const useRollCallsByLaoId = (): {
    [laoId: string]: { [rollCallId: string]: RollCall };
  } => {
    const { makeEventByTypeSelector } = useRollCallContext();
    const eventStatesByLaoIdSelector = useMemo(
      () =>
        makeEventByTypeSelector(RollCall.EVENT_TYPE) as (
          state: unknown,
        ) => Record<string, Record<string, RollCallFeature.EventState>>,
      [makeEventByTypeSelector],
    );

    const eventStatesByLaoId = useSelector(eventStatesByLaoIdSelector);

    // retrieve all event ids from the 2 level map
    const allEventIds = useMemo(
      () => Object.values(eventStatesByLaoId).flatMap(Object.keys),
      [eventStatesByLaoId],
    );

    // retrieve all corresponding roll calls
    const rollCallByIdSelector = useMemo(
      () => makeRollCallByIdSelector(allEventIds),
      [allEventIds],
    );
    const rollCallById = useSelector(rollCallByIdSelector);

    // create a map from laoIds to a map from rollcall id to roll call instances
    return useMemo(
      () => getRollCallsByLaoId(eventStatesByLaoId, rollCallById),
      [eventStatesByLaoId, rollCallById],
    );
  };
}
