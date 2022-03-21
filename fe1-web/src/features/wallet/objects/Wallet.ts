import { Hash, PopToken } from 'core/objects';
import { getStore } from 'core/redux';
import { LaoEventType } from 'features/events/objects/LaoEvent';
import { makeEventByTypeSelector } from 'features/events/reducer';
import { RollCall } from 'features/rollCall/objects';

import { RollCallToken } from './RollCallToken';
import { generateToken } from './Token';

/**
 * Recovers all PoP tokens associated with this wallet.
 *
 * @remarks
 * This is implemented by checking through all known Roll Calls of all known LAOs,
 * generating tokens for them and checking if the token is a verified attendee.
 */
export async function recoverWalletPoPTokens(): Promise<Record<string, Record<string, PopToken>>> {
  const rollCallSelector = makeEventByTypeSelector<RollCall>(LaoEventType.ROLL_CALL);
  // we're outside a component, we can't useSelector
  const rollCalls = rollCallSelector(getStore().getState());

  const tokens: Record<string, Record<string, PopToken>> = {};

  let ops: Promise<any>[] = [];
  for (const [laoId, laoRollCalls] of Object.entries(rollCalls)) {
    tokens[laoId] = {};

    // for each roll call
    const newOps = Object.values(laoRollCalls).map(
      // generate a token
      (rc) =>
        generateToken(new Hash(laoId), rc.id).then((token) => {
          // if it's present in the roll call, add it
          if (rc.containsToken(token)) {
            tokens[laoId][rc.id.valueOf()] = token;
          }
        }),
    );

    ops = ops.concat(newOps);
  }

  await Promise.all(ops);

  return tokens;
}
/**
 * Recovers all PoP tokens associated with this wallet.
 * @return Promise<RollCallToken[]>
 * @remarks
 * This is implemented by checking through all known Roll Calls of all known LAOs,
 * generating tokens for them and checking if the token is a verified attendee.
 */
export async function recoverWalletRollCallTokens(): Promise<RollCallToken[]> {
  const rollCallSelector = makeEventByTypeSelector<RollCall>(LaoEventType.ROLL_CALL);
  // we're outside a component, we can't useSelector
  const rollCalls = rollCallSelector(getStore().getState());

  const tokens: RollCallToken[] = [];

  let ops: Promise<any>[] = [];
  for (const [laoId, laoRollCalls] of Object.entries(rollCalls)) {
    // for each roll call
    const newOps = Object.values(laoRollCalls).map(
      // generate a token
      (rc) =>
        generateToken(new Hash(laoId), rc.id).then((token) => {
          // if it's present in the roll call, add it
          if (rc.containsToken(token)) {
            tokens.push(
              RollCallToken.fromState({
                token: token.toState(),
                laoId: laoId,
                rollCallId: rc.name,
              }),
            );
          }
        }),
    );

    ops = ops.concat(newOps);
  }

  await Promise.all(ops);

  return tokens;
}
