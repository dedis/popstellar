import { mockLao, mockRollCallState } from '__tests__/utils/TestUtils';
import { EventTags, Hash } from 'core/objects';
import { dispatch } from 'core/redux';
import { addEvent, removeEvent } from 'features/events/reducer';
import { connectToLao, disconnectFromLao, removeLao } from 'features/lao/reducer';
import { RollCall } from 'features/rollCall/objects';

import { generateToken } from '../Token';

const createRollCall = (id: string, name: string, mockAttendees: string[]) => {
  return RollCall.fromState({
    ...mockRollCallState,
    id: id,
    name: name,
    attendees: [...mockRollCallState.attendees, ...mockAttendees],
  });
};

const mockRCName0: string = 'mock0';
const mockRCName1: string = 'mock1';

const hashMock0 = Hash.fromStringArray(
  EventTags.ROLL_CALL,
  mockLao.id.valueOf(),
  mockRollCallState.start.valueOf(),
  mockRCName0,
);
const hashMock1 = Hash.fromStringArray(
  EventTags.ROLL_CALL,
  mockLao.id.valueOf(),
  mockRollCallState.start.valueOf(),
  mockRCName1,
);
/*
 * Generates a mock state with some mock popTokens
 */
export async function createMockWalletState() {
  const tokenMockRC0 = await generateToken(mockLao.id, hashMock0);
  const tokenMockRC1 = await generateToken(mockLao.id, hashMock1);
  const mockRollCall0 = createRollCall(hashMock0.valueOf(), mockRCName0, [
    tokenMockRC0.publicKey.valueOf(),
  ]);
  const mockRollCall1 = createRollCall(hashMock1.valueOf(), mockRCName1, [
    tokenMockRC1.publicKey.valueOf(),
  ]);
  dispatch(connectToLao(mockLao.toState()));
  dispatch(addEvent(mockLao.id, mockRollCall0.toState()));
  dispatch(addEvent(mockLao.id, mockRollCall1.toState()));
  console.debug('Dispatched mock events');
}
/*
 * Clears the mock state
 */
export function clearMockWalletState() {
  dispatch(removeEvent(mockLao.id, hashMock0));
  dispatch(removeEvent(mockLao.id, hashMock1));
  dispatch(disconnectFromLao());
  dispatch(removeLao(mockLao.id));
}
