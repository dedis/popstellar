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
    return useMemo(() => {
      const map: {
        [laoId: string]: { [rollCallId: string]: RollCall };
      } = {};

      for (const laoId of Object.keys(eventStatesByLaoId)) {
        map[laoId] = {};

        for (const rollCallId of Object.keys(eventStatesByLaoId[laoId])) {
          map[laoId][rollCallId] = rollCallById[rollCallId];
        }
      }

      return map;
    }, [eventStatesByLaoId, rollCallById]);
  };
}
