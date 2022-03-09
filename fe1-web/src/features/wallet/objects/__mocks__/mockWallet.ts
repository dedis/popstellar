import { mockLao, mockRollCallState } from '__tests__/utils/TestUtils';
import { addEvent, removeEvent } from '../../../events/reducer';
import { RollCall } from '../../../rollCall/objects';
import { connectToLao, disconnectFromLao } from '../../../lao/reducer';
import { dispatch } from '../../../../core/redux';
import { generateToken } from '../Token';
import { Hash } from '../../../../core/objects';

const createRollCall = (Id: string, mockAttendees: string[]) => {
  return RollCall.fromState({
    ...mockRollCallState,
    id: Id,
    name: Id,
    attendees: [...mockRollCallState.attendees, ...mockAttendees],
  });
};
const mockRCID0 = 'mock0';
const mockRCID1 = 'mock1';

// Add attendees, right token from seed and generate stuff
export function useMockWalletState() {
  const publicKeyMockRC0 = generateToken(mockLao.id, new Hash(mockRCID0));
  const publicKeyMockRC1 = generateToken(mockLao.id, new Hash(mockRCID1));

  return {
    useMock: async () => {
      const mockRollCall0 = createRollCall(mockRCID0, [
        (await publicKeyMockRC0).publicKey.valueOf(),
      ]);
      const mockRollCall1 = createRollCall(mockRCID1, [
        (await publicKeyMockRC1).publicKey.valueOf(),
      ]);
      dispatch(connectToLao(mockLao.toState()));
      dispatch(addEvent(mockLao.id, mockRollCall0.toState()));
      dispatch(addEvent(mockLao.id, mockRollCall1.toState()));
      console.debug('Dispatched events');
    },
    clearMock: () => {
      dispatch(removeEvent(mockLao.id, mockRCID0));
      dispatch(removeEvent(mockLao.id, mockRCID1));
      dispatch(disconnectFromLao());
    },
  };
}
