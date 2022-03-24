import { mockLao, mockRollCallState } from '__tests__/utils/TestUtils';

import { Hash } from '../../../../core/objects';
import { dispatch } from '../../../../core/redux';
import { addEvent, removeEvent } from '../../../events/reducer';
import { connectToLao, disconnectFromLao } from '../../../lao/reducer';
import { RollCall } from '../../../rollCall/objects';
import { generateToken } from '../Token';

const createRollCall = (Id: string, mockAttendees: string[]) => {
  return RollCall.fromState({
    ...mockRollCallState,
    id: Id,
    name: Id,
    attendees: [...mockRollCallState.attendees, ...mockAttendees],
  });
};
const mockRCID0: string = '0mock';
const mockRCID1: string = '1mock';
const hashMock0 = new Hash(mockRCID0);
const hashMock1 = new Hash(mockRCID1);
/*
 * Generates a mock state with some mock popTokens
 */
export function useMockWalletState() {
  const tokenMockRC0 = generateToken(mockLao.id, hashMock0);
  const tokenMockRC1 = generateToken(mockLao.id, hashMock1);
  return {
    useMock: async () => {
      const mockRollCall0 = createRollCall(mockRCID0, [(await tokenMockRC0).publicKey.valueOf()]);
      const mockRollCall1 = createRollCall(mockRCID1, [(await tokenMockRC1).publicKey.valueOf()]);
      dispatch(connectToLao(mockLao.toState()));
      dispatch(addEvent(mockLao.id, mockRollCall0.toState()));
      dispatch(addEvent(mockLao.id, mockRollCall1.toState()));
      console.debug('Dispatched mock events');
    },
    clearMock: () => {
      dispatch(removeEvent(mockLao.id, mockRCID0));
      dispatch(removeEvent(mockLao.id, mockRCID1));
      dispatch(disconnectFromLao());
    },
  };
}
