import { mockLao, mockRollCallState } from '__tests__/utils/TestUtils';
import { Hash } from 'core/objects';
import { dispatch } from 'core/redux';
import { addEvent, removeEvent } from 'features/events/reducer';
import { connectToLao, disconnectFromLao } from 'features/lao/reducer';
import { RollCall } from 'features/rollCall/objects';

import { generateToken } from '../Token';

const createRollCall = (id: string, mockAttendees: string[]) => {
  return RollCall.fromState({
    ...mockRollCallState,
    id: id,
    name: id,
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
export async function createMockWalletState() {
  const tokenMockRC0 = await generateToken(mockLao.id, hashMock0);
  const tokenMockRC1 = await generateToken(mockLao.id, hashMock1);
  const mockRollCall0 = createRollCall(mockRCID0, [tokenMockRC0.publicKey.valueOf()]);
  const mockRollCall1 = createRollCall(mockRCID1, [tokenMockRC1.publicKey.valueOf()]);
  dispatch(connectToLao(mockLao.toState()));
  dispatch(addEvent(mockLao.id, mockRollCall0.toState()));
  dispatch(addEvent(mockLao.id, mockRollCall1.toState()));
  console.debug('Dispatched mock events');
}
/*
 * Clears the mock state
 */
export function clearMockWalletState() {
  dispatch(removeEvent(mockLao.id, mockRCID0));
  dispatch(removeEvent(mockLao.id, mockRCID1));
  dispatch(disconnectFromLao());
}
