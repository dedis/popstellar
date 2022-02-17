import React from 'react';
import { render } from '@testing-library/react-native';

import { LaoEventType } from 'model/objects';

import { Meeting } from '../../objects/Meeting';
import EventMeeting from '../EventMeeting';

const meeting = Meeting.fromState({
  eventType: LaoEventType.MEETING,
  id: '1234',
  name: 'MyMeeting',
  location: 'Lausanne',
  creation: 1620255600,
  lastModified: 1620255600,
  start: 1620255600,
  end: 1620255800,
  extra: {},
});

beforeAll(() => {
  jest.useFakeTimers('modern');
  jest.setSystemTime(new Date(1620255600000)); // 5 May 2021
});

describe('EventMeeting', () => {
  it('renders correctly', () => {
    const component = render(<EventMeeting event={meeting} />).toJSON();
    expect(component).toMatchSnapshot();
  });
});
