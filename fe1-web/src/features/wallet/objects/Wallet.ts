import { Lao } from 'features/lao/objects';
import { RollCall } from 'features/rollCall/objects';

import { RollCallToken } from './RollCallToken';
import { generateToken } from './Token';

/**
 * Recovers all PoP tokens associated with this wallet.
 * @return Promise<RollCallToken[]>
 */
export async function recoverWalletRollCallTokens(
  rollCalls: Record<string, Record<string, RollCall>>,
  currentLao: Lao,
): Promise<RollCallToken[]> {
  // For all the roll calls of the current lao
  const tokens = Object.values(rollCalls[currentLao.id.valueOf()]).map((rc) => {
    // Generate the token corresponding to this roll call
    return generateToken(currentLao.id, rc.id).then((popToken) => {
      // If the token participated in the roll call, create a RollCallToken object
      if (rc.containsToken(popToken)) {
        return new RollCallToken({
          token: popToken,
          laoId: currentLao.id,
          rollCallId: rc.id,
          rollCallName: rc.name,
        });
      }
      return undefined;
    });
  });
  return (await Promise.all(tokens)).filter((rct) => rct !== undefined) as RollCallToken[];
}
