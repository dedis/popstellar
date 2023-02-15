import 'jest-extended';
import '__tests__/utils/matchers';
import { mockAddress, mockChannel, mockPopToken } from '__tests__/utils';
import {
  ActionType,
  MessageData,
  ObjectType,
  ProcessableMessage,
} from 'core/network/jsonrpc/messages';
import { Base64UrlData, Hash, Signature, Timestamp } from 'core/objects';
import { RollCall, RollCallStatus } from 'features/rollCall/objects';

import { CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall } from '../messages';
import {
  handleRollCallCloseMessage,
  handleRollCallCreateMessage,
  handleRollCallOpenMessage,
  handleRollCallReopenMessage,
} from '../RollCallHandler';

jest.mock('core/network/JsonRpcApi');
jest.mock('core/redux');
jest.mock('features/lao/reducer');

// region Mock Values Initialization region
const ID = new Hash('rollCallId');
const NAME = 'myRollCall';
const LOCATION = 'location';
const TIMESTAMP_START = new Timestamp(1620355600);
const TIMESTAMP_END = new Timestamp(1620357600);
const ATTENDEES = ['attendee1', 'attendee2'];
const rollCallStateCreated: any = {
  id: ID.valueOf(),
  eventType: RollCall.EVENT_TYPE,
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
  closedAt: undefined,
};

const mockRollCallClosed = RollCall.fromState(rollCallStateClosed);
const mockRollCallCreated = RollCall.fromState(rollCallStateCreated);
const mockRollCallOpened = RollCall.fromState(rollCallStateOpened);
const mockRollCallReopened = RollCall.fromState(rollCallStateReopened);

const createMockMsg = (
  type: ActionType,
  rollCallState: any,
  objectType?: ObjectType,
): ProcessableMessage => {
  const rollCall = RollCall.fromState(rollCallState) as any;

  return {
    laoId: ID,
    receivedAt: TIMESTAMP_START,
    receivedFrom: mockAddress,
    channel: mockChannel,
    data: Base64UrlData.encode(''),
    sender: ID,
    signature: new Signature(''),
    message_id: ID,
    witness_signatures: [],
    messageData: {
      object: objectType || ObjectType.ROLL_CALL,
      action: type,
      ...rollCall,
      proposed_start: rollCall.proposedStart,
      proposed_end: rollCall.proposedEnd,
      opened_at: rollCall?.openedAt,
      update_id: rollCall?.idAlias,
      closed_at: rollCall?.closedAt,
    },
  };
};
// endregion

beforeEach(() => {
  jest.clearAllMocks();
});

describe('RollCallHandler', () => {
  describe('handleRollCallCreateMessage', () => {
    it('should return false for wrong object types', () => {
      expect(
        handleRollCallCreateMessage(jest.fn())(
          createMockMsg(
            ActionType.CREATE,
            {
              ...rollCallStateCreated,
            } as MessageData,
            ObjectType.CHIRP,
          ),
        ),
      ).toBeFalse();
    });

    it('should return false for wrong action types', () => {
      expect(
        handleRollCallCreateMessage(jest.fn())(createMockMsg(ActionType.ADD, rollCallStateCreated)),
      ).toBeFalse();
    });

    it('should return false if the message is not received on a lao channel', () => {
      expect(
        handleRollCallCreateMessage(jest.fn())({
          ...createMockMsg(ActionType.CREATE, rollCallStateCreated),
          laoId: undefined,
        }),
      ).toBeFalse();
    });

    it('should return false if something is off with the message data', () => {
      const message = createMockMsg(ActionType.CREATE, rollCallStateCreated);
      expect(
        handleRollCallCreateMessage(jest.fn())({
          ...message,
          messageData: {
            ...message.messageData,
            id: undefined as unknown as Hash,
          } as CreateRollCall,
        }),
      ).toBeFalse();
    });

    it('should create a correct RollCall object from msgData', async () => {
      const usedMockMsg = createMockMsg(ActionType.CREATE, rollCallStateCreated);

      const mockAddEvent = jest.fn();

      expect(handleRollCallCreateMessage(mockAddEvent)(usedMockMsg)).toBeTrue();

      expect(mockAddEvent).toHaveBeenCalledWith(usedMockMsg.laoId, mockRollCallCreated);
    });
  });

  describe('handleRollCallOpenMessage', () => {
    it('should return false for wrong object types', () => {
      expect(
        handleRollCallOpenMessage(
          jest.fn(),
          jest.fn(),
        )(
          createMockMsg(
            ActionType.CREATE,
            {
              ...rollCallStateOpened,
            } as MessageData,
            ObjectType.CHIRP,
          ),
        ),
      ).toBeFalse();
    });

    it('should return false for wrong action types', () => {
      expect(
        handleRollCallOpenMessage(
          jest.fn(),
          jest.fn(),
        )(createMockMsg(ActionType.ADD, rollCallStateOpened)),
      ).toBeFalse();
    });

    it('should return false if the message is not received on a lao channel', () => {
      expect(
        handleRollCallOpenMessage(
          jest.fn(),
          jest.fn(),
        )({ ...createMockMsg(ActionType.OPEN, rollCallStateOpened), laoId: undefined }),
      ).toBeFalse();
    });

    it('should return false for unkown roll call ids', () => {
      expect(
        handleRollCallOpenMessage(
          jest.fn(() => undefined),
          jest.fn(),
        )(createMockMsg(ActionType.OPEN, rollCallStateOpened)),
      ).toBeFalse();
    });

    it('should return false if there is an issue with the message data', () => {
      const message = createMockMsg(ActionType.OPEN, rollCallStateOpened);
      expect(
        handleRollCallOpenMessage(
          () => RollCall.fromState(mockRollCallCreated.toState()),
          jest.fn(),
        )({
          ...message,
          messageData: {
            ...message.messageData,
            update_id: undefined as unknown as Hash,
          } as OpenRollCall,
        }),
      ).toBeFalse();
    });

    it('should create a correct RollCall object from msgData', async () => {
      const usedMockMsg = createMockMsg(ActionType.OPEN, rollCallStateOpened);

      const mockGetEventById = jest.fn(() => RollCall.fromState(mockRollCallCreated.toState()));
      const mockUpdateEvent = jest.fn();

      expect(handleRollCallOpenMessage(mockGetEventById, mockUpdateEvent)(usedMockMsg)).toBeTrue();

      expect(mockUpdateEvent).toHaveBeenCalledWith(mockRollCallOpened);
    });
  });

  describe('handleRollCallCloseMessage', () => {
    it('should return false for wrong object types', () => {
      expect(
        handleRollCallCloseMessage(
          jest.fn(),
          jest.fn(),
          jest.fn(),
          jest.fn(),
        )(
          createMockMsg(
            ActionType.CREATE,
            {
              ...rollCallStateClosed,
            } as MessageData,
            ObjectType.CHIRP,
          ),
        ),
      ).toBeFalse();
    });

    it('should return false for wrong action types', () => {
      expect(
        handleRollCallCloseMessage(
          jest.fn(),
          jest.fn(),
          jest.fn(),
          jest.fn(),
        )(createMockMsg(ActionType.ADD, rollCallStateClosed)),
      ).toBeFalse();
    });

    it('should return false if the message is not received on a lao channel', () => {
      expect(
        handleRollCallCloseMessage(
          jest.fn(),
          jest.fn(),
          jest.fn(),
          jest.fn(),
        )({ ...createMockMsg(ActionType.CLOSE, rollCallStateClosed), laoId: undefined }),
      ).toBeFalse();
    });

    it('should return false for unkown roll call ids', () => {
      expect(
        handleRollCallCloseMessage(
          jest.fn(() => undefined),
          jest.fn(),
          jest.fn(),
          jest.fn(),
        )(createMockMsg(ActionType.CLOSE, rollCallStateOpened)),
      ).toBeFalse();
    });

    it('should return false in case of issues with the message data', () => {
      const message = createMockMsg(ActionType.CLOSE, rollCallStateOpened);

      expect(
        handleRollCallCloseMessage(
          jest.fn(() => RollCall.fromState(mockRollCallOpened.toState())),
          jest.fn(),
          () => Promise.resolve(mockPopToken),
          jest.fn(),
        )({
          ...message,
          messageData: {
            ...message.messageData,
            closed_at: undefined as unknown as Timestamp,
          } as CloseRollCall,
        }),
      ).toBeFalse();
    });

    it('should create a correct RollCall object from msgData in handleRollCallCloseMessage', async () => {
      const usedMockMsg = createMockMsg(ActionType.CLOSE, rollCallStateClosed);

      const mockGetEventById = jest.fn(() => RollCall.fromState(mockRollCallOpened.toState()));
      const mockUpdateEvent = jest.fn();
      const mockGenerateToken = jest.fn(() => Promise.resolve(mockPopToken));
      const mockSetLaoLastRollCall = jest.fn();

      expect(
        handleRollCallCloseMessage(
          mockGetEventById,
          mockUpdateEvent,
          mockGenerateToken,
          mockSetLaoLastRollCall,
        )(usedMockMsg),
      ).toBeTrue();

      expect(mockUpdateEvent).toHaveBeenCalledWith(mockRollCallClosed);
    });
  });

  describe('handleRollCallReopenMessage', () => {
    it('should return false for wrong object types', () => {
      expect(
        handleRollCallReopenMessage(
          jest.fn(),
          jest.fn(),
        )(
          createMockMsg(
            ActionType.CREATE,
            {
              ...rollCallStateClosed,
            } as MessageData,
            ObjectType.CHIRP,
          ),
        ),
      ).toBeFalse();
    });

    it('should return false for wrong action types', () => {
      expect(
        handleRollCallReopenMessage(
          jest.fn(),
          jest.fn(),
        )(createMockMsg(ActionType.ADD, rollCallStateClosed)),
      ).toBeFalse();
    });

    it('should return false for unkown roll call ids', () => {
      expect(
        handleRollCallReopenMessage(
          jest.fn(() => undefined),
          jest.fn(),
        )(createMockMsg(ActionType.REOPEN, rollCallStateOpened)),
      ).toBeFalse();
    });

    it('should return false if the given roll call is not closed', () => {
      expect(
        handleRollCallReopenMessage(
          jest.fn(() => mockRollCallOpened),
          jest.fn(),
        )(createMockMsg(ActionType.REOPEN, rollCallStateOpened)),
      ).toBeFalse();
    });

    it('should return false in case of issues with the message data', () => {
      const message = createMockMsg(ActionType.REOPEN, rollCallStateOpened);
      expect(
        handleRollCallReopenMessage(
          jest.fn(() => mockRollCallClosed),
          jest.fn(),
        )({
          ...message,
          messageData: {
            ...message.messageData,
            opened_at: undefined as unknown as Timestamp,
          } as ReopenRollCall,
        }),
      ).toBeFalse();
    });

    it('should create a correct RollCall object from msgData', async () => {
      const usedMockMsg = createMockMsg(ActionType.REOPEN, rollCallStateReopened);

      const mockGetEventById = jest.fn(() => RollCall.fromState(mockRollCallClosed.toState()));
      const mockUpdateEvent = jest.fn();

      expect(
        handleRollCallReopenMessage(mockGetEventById, mockUpdateEvent)(usedMockMsg),
      ).toBeTrue();

      expect(mockUpdateEvent).toHaveBeenCalledWith(mockRollCallReopened);
    });
  });
});
