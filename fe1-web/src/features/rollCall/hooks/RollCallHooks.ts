import { useContext, useMemo } from 'react';
import { useSelector } from 'react-redux';

import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';
import { RollCallToken } from 'core/objects/RollCallToken';
import { isDefined } from 'core/types';

import { RollCallFeature, RollCallReactContext, ROLLCALL_FEATURE_IDENTIFIER } from '../interface';
import { RollCall } from '../objects';
import { makeRollCallByIdSelector, makeRollCallSelector } from '../reducer';

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

  export const useRollCallById = (rollCallId: Hash | string) => {
    const rollCallSelector = useMemo(
      () => makeRollCallSelector(rollCallId.valueOf()),
      [rollCallId],
    );
    return useSelector(rollCallSelector);
  };

  /**
   * Gets the generateToken function
   */
  export const useGenerateToken = () => useRollCallContext().generateToken;

  /**
   * Gets the hasSeed function
   */
  export const useHasSeed = () => useRollCallContext().hasSeed;

  export const useRollCallsByLaoId = (
    laoId: string,
  ): {
    [rollCallId: string]: RollCall;
  } => {
    const { makeEventByTypeSelector } = useRollCallContext();
    const eventStatesByLaoIdSelector = useMemo(
      () =>
        makeEventByTypeSelector(laoId, RollCall.EVENT_TYPE) as (
          state: unknown,
        ) => Record<string, RollCallFeature.EventState>,
      [makeEventByTypeSelector, laoId],
    );

    const eventStatesByLaoId = useSelector(eventStatesByLaoIdSelector);

    // retrieve all event ids from the map
    const allEventIds = useMemo(() => Object.keys(eventStatesByLaoId), [eventStatesByLaoId]);

    // retrieve all corresponding roll calls
    const rollCallByIdSelector = useMemo(
      () => makeRollCallByIdSelector(allEventIds),
      [allEventIds],
    );

    return useSelector(rollCallByIdSelector);
  };

  export const useRollCallTokensByLaoId = (laoId: string): Promise<RollCallToken[]> => {
    const rollCalls = useRollCallsByLaoId(laoId);
    const generate = useRollCallContext().generateToken;
    const rollCallTokensSelector = useMemo(async () => {
      const laoIdHash = new Hash(laoId);
      const tokens = Object.values(rollCalls).map((rc) =>
        generate(laoIdHash, rc.id).then((popToken) => {
          // If the token participated in the roll call, create a RollCallToken object
          if (rc.containsToken(popToken)) {
            return new RollCallToken({
              token: popToken,
              laoId: laoIdHash,
              rollCallId: rc.id,
              rollCallName: rc.name,
            });
          }
          return undefined;
        }),
      );
      return (await Promise.all(tokens)).filter(isDefined);
    }, [laoId, rollCalls, generate]);
    return useSelector(() => rollCallTokensSelector);
  };

  export const useRollCallTokenByRollCallId = (
    laoId: string,
    rollCallId: string,
  ): Promise<RollCallToken | undefined> => {
    const rollCallSelector = useMemo(() => makeRollCallSelector(rollCallId), [rollCallId]);
    const rollCall = useSelector(rollCallSelector);
    const generate = useRollCallContext().generateToken;
    const tokenSelector = useMemo(async () => {
      if (!rollCall) return undefined;
      const token = await generate(new Hash(laoId), rollCall.id).then(
        (popToken): RollCallToken => ({
          rollCallId: rollCall.id,
          rollCallName: rollCall.name,
          token: popToken,
          laoId: new Hash(laoId),
        }),
      );
      return token;
    }, [generate, rollCall, laoId]);
    return useSelector(() => tokenSelector);
  };
}
