import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import {
  Hash, LaoState, PublicKey, Timestamp, Lao,
} from 'model/objects';
import { Chirp } from 'model/objects/Chirp';
import { OpenedLaoStore } from 'store';
import { requestDeleteChirp as mockRequestDeleteChirp } from 'network/MessageApi';
import ChirpCard from '../ChirpCard';

const TIMESTAMP = 1609455600;
const laoState: LaoState = {
  id: '1234',
  name: 'MyLao',
  creation: TIMESTAMP,
  last_modified: TIMESTAMP,
  organizer: '1234',
  witnesses: [],
};

const sender = new PublicKey('Douglas Adams');
const chirp = new Chirp({
  id: new Hash('1234'),
  text: 'Don\'t panic.',
  sender: sender,
  time: new Timestamp(1609455600), // 31 December 2020
  isDeleted: false,
});

const deletedChirp = new Chirp({
  id: new Hash('1234'),
  text: 'Don\'t panic.',
  sender: sender,
  time: new Timestamp(1609455600), // 31 December 2020
  isDeleted: true,
});

jest.mock('network/MessageApi');
jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn().mockImplementation(() => laoState),
}));

beforeAll(() => {
  jest.useFakeTimers('modern');
  jest.setSystemTime(new Date(1620255600000)); // 5 May 2021
});

describe('ChirpCard', () => {
  it('renders correctly for sender', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => Lao.fromState(laoState));
    const obj = render(
      <ChirpCard chirp={chirp} userPublicKey={new PublicKey('Douglas Adams')} />,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('renders correctly for non-sender', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => Lao.fromState(laoState));
    const obj = render(
      <ChirpCard chirp={chirp} userPublicKey={new PublicKey('IAmNotTheSender')} />,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('calls delete correctly', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => Lao.fromState(laoState));
    const button = render(
      <ChirpCard chirp={chirp} userPublicKey={new PublicKey('Douglas Adams')} />,
    ).getByLabelText('delete');
    fireEvent.press(button);
    expect(mockRequestDeleteChirp).toHaveBeenCalledTimes(1);
  });

  it('render correct for a deleted chirp', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => Lao.fromState(laoState));
    const obj = render(
      <ChirpCard chirp={deletedChirp} userPublicKey={new PublicKey('Douglas Adams')} />,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });
});

afterAll(() => {
  jest.useRealTimers();
});
