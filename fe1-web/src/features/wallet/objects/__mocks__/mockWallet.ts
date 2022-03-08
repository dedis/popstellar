import { mockLao, mockRollCallState } from '__tests__/utils/TestUtils';
import { addEvent, removeEvent } from '../../../events/reducer';
import { RollCall } from '../../../rollCall/objects';
import { connectToLao, disconnectFromLao } from '../../../lao/reducer';
import { dispatch } from '../../../../core/redux';

const createRollCallWithId = (Id: string) => {
  return RollCall.fromState({
    ...mockRollCallState,
    id: Id,
  });
};

export function useMockWalletState() {
  const mockRollCall0 = createRollCallWithId('mock0');
  const mockRollCall1 = createRollCallWithId('mock1');
  return {
    useMock: () => {
      dispatch(connectToLao(mockLao.toState()));
      dispatch(addEvent(mockLao.id, mockRollCall0.toState()));
      dispatch(addEvent(mockLao.id, mockRollCall1.toState()));
      console.debug('Dispatched events');
    },
    clearMock: () => {
      dispatch(removeEvent(mockLao.id, mockRollCall0.id.valueOf()));
      dispatch(removeEvent(mockLao.id, mockRollCall1.id.valueOf()));
      dispatch(disconnectFromLao());
    },
  };
}
