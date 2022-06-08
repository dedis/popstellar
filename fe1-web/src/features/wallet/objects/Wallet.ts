import { Hash } from 'core/objects';
import { isDefined } from 'core/types';

import { WalletFeature } from '../interface';
import { RollCallToken } from './RollCallToken';
import { generateToken } from './Token';

/**
 * Recovers all PoP tokens associated with this wallet.
 * @return Promise<RollCallToken[]>
 */
export async function recoverWalletRollCallTokens(
  rollCalls: WalletFeature.RollCall[],
  laoId: Hash,
): Promise<RollCallToken[]> {
  // For all the roll calls of the current lao
  const tokens = rollCalls.map((rc) =>
    generateToken(laoId, rc.id).then((popToken) => {
      // If the token participated in the roll call, create a RollCallToken object
      if (rc.containsToken(popToken)) {
        return new RollCallToken({
          token: popToken,
          laoId: laoId,
          rollCallId: rc.id,
          rollCallName: rc.name,
        });
      }
      return undefined;
    }),
  );

  return (await Promise.all(tokens)).filter(isDefined);
}
