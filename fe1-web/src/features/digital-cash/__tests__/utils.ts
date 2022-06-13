import { mockKeyPair, mockLaoIdHash } from '__tests__/utils';
import { Hash, PopToken, RollCallToken } from 'core/objects';

import {
  DIGITAL_CASH_FEATURE_IDENTIFIER,
  DigitalCashFeature,
  DigitalCashReactContext,
} from '../interface';

export const mockRollCall = {
  id: new Hash('rcid'),
  name: 'rc',
  attendees: [mockKeyPair.publicKey],
  containsToken: () => true,
} as DigitalCashFeature.RollCall;

export const mockRollCallToken = new RollCallToken({
  laoId: mockLaoIdHash,
  rollCallName: mockRollCall.name,
  rollCallId: mockRollCall.id,
  token: PopToken.fromState(mockKeyPair.toState()),
});

export const mockDigitalCashContextValue = (isOrganizer: boolean) => ({
  [DIGITAL_CASH_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    useIsLaoOrganizer: () => isOrganizer,
    useRollCallById: () => mockRollCall,
    useRollCallTokensByLaoId: () => [mockRollCallToken],
    useRollCallTokenByRollCallId: () => mockRollCallToken,
  } as DigitalCashReactContext,
});
