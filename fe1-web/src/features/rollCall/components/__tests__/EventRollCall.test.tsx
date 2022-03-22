import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';
import * as reactRedux from 'react-redux';

import { mockNavigate } from '__mocks__/useNavigationMock';
import { mockLao } from '__tests__/utils/TestUtils';
import { Hash, Timestamp } from 'core/objects';
import STRINGS from 'resources/strings';

import { requestOpenRollCall, requestReopenRollCall } from '../../network';
import { EventTypeRollCall, RollCall, RollCallStatus } from '../../objects';
import EventRollCall from '../EventRollCall';

const ID = new Hash('rollCallId');
const NAME = 'myRollCall';
const LOCATION = 'location';
const TIMESTAMP_START = new Timestamp(1620255600);
const TIMESTAMP_END = new Timestamp(1620357600);
const ATTENDEES = ['attendee1', 'attendee2'];

const createStateWithStatus: any = (mockStatus: RollCallStatus) => {
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

const mockRollCallClosed = RollCall.fromState(createStateWithStatus(RollCallStatus.CLOSED));
const mockRollCallCreated = RollCall.fromState(createStateWithStatus(RollCallStatus.CREATED));
const mockRollCallOpened = RollCall.fromState(createStateWithStatus(RollCallStatus.OPENED));

jest.mock('@react-navigation/native');
jest.mock('features/rollCall/network', () => ({
  requestOpenRollCall: jest.fn(() => Promise.resolve()),
  requestReopenRollCall: jest.fn(() => Promise.resolve()),
}));

let mockSelector: any;

beforeEach(() => {
  jest.clearAllMocks();
  mockSelector = jest.spyOn(reactRedux, 'useSelector').mockReturnValueOnce(mockLao);
});

describe('EventRollCall', () => {
  it('should correctly render', () => {
    mockSelector.mockReturnValueOnce(mockRollCallCreated);
    const obj = render(<EventRollCall event={mockRollCallCreated} isOrganizer={false} />);
    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('should call requestOpenRollCall when the open button is clicked', () => {
    const usedMockRollCall = mockRollCallCreated;
    mockSelector.mockReturnValueOnce(usedMockRollCall);
    const obj = render(<EventRollCall event={usedMockRollCall} isOrganizer />);
    const openRollCallButton = obj.getByText(STRINGS.roll_call_open);
    fireEvent.press(openRollCallButton);
    expect(requestOpenRollCall).toHaveBeenCalledTimes(1);
  });

  it('should call requestReopenRollCall when the reopen button is clicked', () => {
    const usedMockRollCall = mockRollCallClosed;
    mockSelector.mockReturnValueOnce(usedMockRollCall);
    const obj = render(<EventRollCall event={usedMockRollCall} isOrganizer />);
    const reopenRollCallButton = obj.getByText(STRINGS.roll_call_reopen);
    fireEvent.press(reopenRollCallButton);
    expect(requestReopenRollCall).toHaveBeenCalledTimes(1);
  });

  it('should navigate to RollCallOpened when scan attendees button is clicked', () => {
    const usedMockRollCall = mockRollCallOpened;
    mockSelector.mockReturnValueOnce(usedMockRollCall);
    const obj = render(<EventRollCall event={usedMockRollCall} isOrganizer />);
    const scanAttendeesButton = obj.getByText(STRINGS.roll_call_scan_attendees);
    fireEvent.press(scanAttendeesButton);
    expect(mockNavigate).toHaveBeenCalledTimes(1);
  });
});
