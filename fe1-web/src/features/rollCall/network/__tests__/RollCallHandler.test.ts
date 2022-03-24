import 'jest-extended';
import '__tests__/utils/matchers';
import { mockLao } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Base64UrlData, Hash, Signature, Timestamp } from 'core/objects';
import { getStore } from 'core/redux';
import { selectEventById } from 'features/events/network/EventHandlerUtils';
import { addEvent, updateEvent } from 'features/events/reducer';
import { selectCurrentLao } from 'features/lao/reducer';
import { EventTypeRollCall, RollCall, RollCallStatus } from 'features/rollCall/objects';

import {
  handleRollCallCloseMessage,
  handleRollCallCreateMessage,
  handleRollCallOpenMessage,
  handleRollCallReopenMessage,
} from '../RollCallHandler';

jest.mock('core/network/JsonRpcApi');
jest.mock('features/events/reducer');
jest.mock('core/redux');
jest.mock('features/events/network/EventHandlerUtils');
jest.mock('features/lao/reducer');

// region Mock Values Initialization region
const ID = new Hash('rollCallId');
const NAME = 'myRollCall';
const LOCATION = 'location';
const TIMESTAMP_START = new Timestamp(1620255600);
const TIMESTAMP_END = new Timestamp(1620357600);
const ATTENDEES = ['attendee1', 'attendee2'];
const rollCallStateCreated: any = {
  id: ID.valueOf(),
  eventType: EventTypeRollCall,
  start: TIMESTAMP_START.valueOf(),
  name: NAME,
  location: LOCATION,
  creation: TIMESTAMP_START.valueOf(),
  proposedStart: TIMESTAMP_START.valueOf(),
  proposedEnd: TIMESTAMP_END.valueOf(),
  status: RollCallStatus.CREATED,
};
const rollCallStateOpened = {
  ...rollCallStateCreated,
  idAlias: ID.valueOf(),
  openedAt: TIMESTAMP_START.valueOf(),
  status: RollCallStatus.OPENED,
};
const rollCallStateClosed = {
  ...rollCallStateOpened,
  closedAt: TIMESTAMP_END.valueOf(),
  status: RollCallStatus.CLOSED,
  attendees: ATTENDEES,
};
const rollCallStateReopened = {
  ...rollCallStateClosed,
  status: RollCallStatus.REOPENED,
};

const mockRollCallClosed = RollCall.fromState(rollCallStateClosed);
const mockRollCallCreated = RollCall.fromState(rollCallStateCreated);
const mockRollCallOpened = RollCall.fromState(rollCallStateOpened);
const mockRollCallReopened = RollCall.fromState(rollCallStateReopened);

const createMockMsg = (type: ActionType, status: RollCallStatus, rollCallState: any) => {
  return {
    laoId: ID,
    receivedAt: TIMESTAMP_START,
    channel: 'undefined',
    data: Base64UrlData.encode(''),
    sender: ID,
    signature: new Signature(''),
    message_id: ID,
    witness_signatures: [],
    messageData: {
      object: ObjectType.ROLL_CALL,
      action: type,
      ...rollCallState,
      proposed_start: rollCallState.proposedStart,
      proposed_end: rollCallState.proposedEnd,
      opened_at: rollCallState?.openedAt,
      update_id: rollCallState?.idAlias,
      closed_at: rollCallState?.closedAt,
    },
  };
};
// endregion

beforeEach(() => {
  jest.clearAllMocks();
  (getStore as jest.Mock).mockImplementation(() => {
    return {
      getState: jest.fn(),
    };
  });
  (selectCurrentLao as unknown as jest.Mock).mockReturnValue(mockLao);
});

describe('RollCallHandler', () => {
  it('should create a correct RollCall object from msgData in handleRollCallCreateMessage', async () => {
    const usedMockMsg = createMockMsg(
      ActionType.CREATE,
      RollCallStatus.CREATED,
      rollCallStateCreated,
    );
    handleRollCallCreateMessage(usedMockMsg);
    expect(addEvent).toHaveBeenCalledWith(usedMockMsg.laoId, mockRollCallCreated.toState());
  });

  it('should create a correct RollCall object from msgData in handleRollCallOpenMessage', async () => {
    const usedMockMsg = createMockMsg(ActionType.OPEN, RollCallStatus.OPENED, rollCallStateOpened);
    (selectEventById as jest.Mock).mockReturnValue(mockRollCallCreated);
    handleRollCallOpenMessage(usedMockMsg);
    expect(updateEvent).toHaveBeenCalledWith(usedMockMsg.laoId, mockRollCallOpened.toState());
  });

  it('should create a correct RollCall object from msgData in handleRollCallCloseMessage', async () => {
    const usedMockMsg = createMockMsg(ActionType.CLOSE, RollCallStatus.CLOSED, rollCallStateClosed);
    (selectEventById as jest.Mock).mockReturnValue(mockRollCallOpened);
    handleRollCallCloseMessage(usedMockMsg);
    expect(updateEvent).toHaveBeenCalledWith(usedMockMsg.laoId, mockRollCallClosed.toState());
  });

  it('should create a correct RollCall object from msgData in handleRollCallReopenMessage', async () => {
    const usedMockMsg = createMockMsg(
      ActionType.REOPEN,
      RollCallStatus.REOPENED,
      rollCallStateReopened,
    );
    (selectEventById as jest.Mock).mockReturnValue(mockRollCallClosed);
    handleRollCallReopenMessage(usedMockMsg);
    expect(updateEvent).toHaveBeenCalledWith(usedMockMsg.laoId, mockRollCallReopened.toState());
  });
});
