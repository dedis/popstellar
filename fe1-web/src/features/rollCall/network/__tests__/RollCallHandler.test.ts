import 'jest-extended';
import '__tests__/utils/matchers';
import { getStore } from 'core/redux';
import { getEventFromId } from 'features/events/network/EventHandlerUtils';
import { Base64UrlData, Hash, Signature, Timestamp } from 'core/objects';
import { addEvent, updateEvent } from 'features/events/reducer';
import { EventTypeRollCall, RollCall, RollCallStatus } from 'features/rollCall/objects';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
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

const mockMsg = (type: ActionType, status: RollCallStatus, rollCallState: any) => {
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

beforeEach(() => {
  jest.resetAllMocks();
  (getStore as jest.Mock).mockImplementation(() => {
    return {
      getState: jest.fn(),
    };
  });
});

describe('RollCallHandler', () => {
  it('should create a correct RollCall object from msgData in handleRollCallCreateMessage', async () => {
    const usedMockMsg = mockMsg(ActionType.CREATE, RollCallStatus.CREATED, rollCallStateCreated);
    const mockCall = addEvent as any;
    handleRollCallCreateMessage(usedMockMsg);
    expect(mockCall).toHaveBeenCalledWith(usedMockMsg.laoId, mockRollCallCreated.toState());
  });
  it('should create a correct RollCall object from msgData in handleRollCallOpenMessage', async () => {
    const usedMockMsg = mockMsg(ActionType.OPEN, RollCallStatus.OPENED, rollCallStateOpened);
    (getEventFromId as jest.Mock).mockReturnValue(mockRollCallCreated);
    const mockCall = updateEvent as any;
    handleRollCallOpenMessage(usedMockMsg);
    expect(mockCall).toHaveBeenCalledWith(usedMockMsg.laoId, mockRollCallOpened.toState());
  });
  it('should create a correct RollCall object from msgData in handleRollCallCloseMessage', async () => {
    const usedMockMsg = mockMsg(ActionType.CLOSE, RollCallStatus.CLOSED, rollCallStateClosed);
    (getEventFromId as jest.Mock).mockReturnValue(mockRollCallOpened);
    const mockCall = updateEvent as any;
    handleRollCallCloseMessage(usedMockMsg);
    expect(mockCall).toHaveBeenCalledWith(usedMockMsg.laoId, mockRollCallClosed.toState());
  });
  it('should create a correct RollCall object from msgData in handleRollCallReopenMessage', async () => {
    const usedMockMsg = mockMsg(ActionType.REOPEN, RollCallStatus.REOPENED, rollCallStateReopened);
    (getEventFromId as jest.Mock).mockReturnValue(mockRollCallClosed);
    const mockCall = updateEvent as any;
    handleRollCallReopenMessage(usedMockMsg);
    expect(mockCall).toHaveBeenCalledWith(usedMockMsg.laoId, mockRollCallReopened.toState());
  });
});
