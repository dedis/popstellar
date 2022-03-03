import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import STRINGS from 'resources/strings';
import { Hash, Timestamp } from 'core/objects';
import { useNavigation } from '@react-navigation/native';

import * as reactRedux from 'react-redux';
import { mockLao } from '__tests__/utils/TestUtils';
import { requestOpenRollCall, requestReopenRollCall } from 'features/rollCall/network';
import EventRollCall from '../EventRollCall';
import { EventTypeRollCall, RollCall, RollCallStatus } from '../../objects';

const ID = new Hash('rollCallId');
const NAME = 'myRollCall';
const LOCATION = 'location';
const TIMESTAMP_START = new Timestamp(1620255600);
const TIMESTAMP_END = new Timestamp(1620357600);
const ATTENDEES = ['attendee1', 'attendee2'];
const rollCallState: any = (mockStatus: RollCallStatus) => {
  return {
    id: ID.valueOf(),
    eventType: EventTypeRollCall,
    start: TIMESTAMP_START.valueOf(),
    name: NAME,
    location: LOCATION,
    creation: TIMESTAMP_START.valueOf(),
    proposedStart: TIMESTAMP_START.valueOf(),
    proposedEnd: TIMESTAMP_END.valueOf(),
    status: mockStatus,
    attendees: ATTENDEES,
    idAlias: mockStatus === RollCallStatus.CREATED ? undefined : ID.valueOf(),
  };
};

const mockRollCallClosed = RollCall.fromState(rollCallState(RollCallStatus.CLOSED));
const mockRollCallCreated = RollCall.fromState(rollCallState(RollCallStatus.CREATED));
const mockRollCallOpened = RollCall.fromState(rollCallState(RollCallStatus.OPENED));

const mockRenderRollCall = (rollCall: RollCall, isOrganizer: boolean) => {
  return render(<EventRollCall event={rollCall} isOrganizer={isOrganizer} />);
};

jest.mock('@react-navigation/native');
jest.mock('features/rollCall/network');
const mockNavigate = jest.fn();

let mockSelector: jest.SpyInstance<
  unknown,
  [
    selector: (state: unknown) => unknown,
    equalityFn?: ((left: unknown, right: unknown) => boolean) | undefined,
  ]
>;

beforeEach(() => {
  jest.resetAllMocks();
  mockSelector = jest.spyOn(reactRedux, 'useSelector').mockReturnValueOnce(mockLao);
  (useNavigation as jest.Mock).mockImplementation(() => {
    return {
      navigate: mockNavigate,
    };
  });
  (requestOpenRollCall as jest.Mock).mockImplementation(() => Promise.resolve());
  (requestReopenRollCall as jest.Mock).mockImplementation(() => Promise.resolve());
});

describe('EventRollCall', () => {
  it('should correctly render', () => {
    mockSelector.mockReturnValueOnce(mockRollCallCreated);
    const obj = mockRenderRollCall(mockRollCallCreated, false);
    expect(obj.toJSON()).toMatchSnapshot();
  });
  it('should call requestOpenRollCall when the open button is clicked', () => {
    const usedMockRollCall = mockRollCallCreated;
    mockSelector.mockReturnValueOnce(usedMockRollCall);
    const obj = mockRenderRollCall(usedMockRollCall, true);
    const openRollCallButton = obj.getByText(STRINGS.roll_call_open);
    fireEvent.press(openRollCallButton);
    expect(requestOpenRollCall).toHaveBeenCalled();
  });
  it('should call requestReopenRollCall when the reopen button is clicked', () => {
    const usedMockRollCall = mockRollCallClosed;
    mockSelector.mockReturnValueOnce(usedMockRollCall);
    const obj = mockRenderRollCall(usedMockRollCall, true);
    const reopenRollCallButton = obj.getByText(STRINGS.roll_call_reopen);
    fireEvent.press(reopenRollCallButton);
    expect(requestReopenRollCall).toHaveBeenCalled();
  });
  it('should navigate to RollCallOpened when scan attendees button is clicked', () => {
    const usedMockRollCall = mockRollCallOpened;
    mockSelector.mockReturnValueOnce(usedMockRollCall);
    const obj = mockRenderRollCall(usedMockRollCall, true);
    const scanAttendeesButton = obj.getByText(STRINGS.roll_call_scan_attendees);
    fireEvent.press(scanAttendeesButton);
    expect(mockNavigate).toHaveBeenCalled();
  });
});
