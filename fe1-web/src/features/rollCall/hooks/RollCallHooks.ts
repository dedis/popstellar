import { useContext, useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';

import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';
import { RollCallToken } from 'core/objects/RollCallToken';
import { isDefined } from 'core/types';

import { ROLLCALL_FEATURE_IDENTIFIER, RollCallFeature, RollCallReactContext } from '../interface';
import { RollCall } from '../objects';
import {
  makeRollCallAttendeesListSelector,
  makeRollCallByIdSelector,
  makeRollCallSelector,
} from '../reducer';

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
   * Gets the current lao id or throws an exception if there is none
   */
  export const useAssertCurrentLaoId = () => useRollCallContext().useAssertCurrentLaoId();

  export const useRollCallById = (rollCallId: Hash | string | undefined) => {
    const rollCallSelector = useMemo(
      () => makeRollCallSelector(rollCallId?.valueOf()),
      [rollCallId],
    );
    return useSelector(rollCallSelector);
  };

  /**
   * Gets the list of attendees for a roll call.
   * @param rollCallId - The id of the roll call
   */
  export const useAttendeesByRollCallId = (rollCallId: Hash | string | undefined) => {
    const rollCallAttendeesSelector = useMemo(
      () => makeRollCallAttendeesListSelector(rollCallId?.valueOf()),
      [rollCallId],
    );
    return useSelector(rollCallAttendeesSelector);
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

  export const useRollCallTokensByLaoId = (laoId: string): RollCallToken[] => {
    const rollCalls = useRollCallsByLaoId(laoId);
    const generate = useRollCallContext().generateToken;

    const [rollCallTokens, setRollCallTokens] = useState<RollCallToken[]>([]);

    useEffect(() => {
      // allows the promise to be cancelled in cases of re-rendering the component
      let wasCanceled = false;

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

      Promise.all(tokens).then((newRollCallTokens) => {
        if (!wasCanceled) {
          setRollCallTokens(newRollCallTokens.filter(isDefined));
        }
      });

      return () => {
        wasCanceled = true;
      };
    }, [laoId, rollCalls, generate]);

    return rollCallTokens;
  };

  export const useRollCallTokenByRollCallId = (
    laoId: string,
    rollCallId: string,
  ): RollCallToken | undefined => {
    const rollCallSelector = useMemo(() => makeRollCallSelector(rollCallId), [rollCallId]);
    const rollCall = useSelector(rollCallSelector);
    const generate = useRollCallContext().generateToken;

    const [rollCallToken, setRollCallToken] = useState<RollCallToken | undefined>(undefined);

    useEffect(() => {
      // allows the promise to be cancelled in cases of re-rendering the component
      let wasCanceled = false;

      if (!rollCall) {
        return undefined;
      }

      generate(new Hash(laoId), rollCall.id)
        .then(
          (popToken): RollCallToken => ({
            rollCallId: rollCall.id,
            rollCallName: rollCall.name,
            token: popToken,
            laoId: new Hash(laoId),
          }),
        )
        .then((newRollCallToken) => {
          if (!wasCanceled) {
            setRollCallToken(newRollCallToken);
          }
        });

      return () => {
        wasCanceled = true;
      };
    }, [laoId, rollCall, generate]);

    return rollCallToken;
  };
}
