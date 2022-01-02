import React from 'react';
import { render } from '@testing-library/react-native';
import {
  Hash, LaoState, PublicKey, Timestamp, Lao,
} from 'model/objects';
import { Chirp } from 'model/objects/Chirp';
import { OpenedLaoStore } from 'store';
import ChirpCard from '../ChirpCard';
// import { requestDeleteChirp as mockRequestDeleteChirp } from 'network/MessageApi';

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
  likes: 42,
  isDeleted: false,
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
    // TODO: mock the sender
    /* const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => Lao.fromState(laoState));
    const getMockSender = jest.spyOn(KeyPairStore, 'getPublicKey');
    getMockSender.mockImplementation(() => sender);
    const obj = render(
      <ChirpCard chirp={chirp} />,
    );
    expect(obj.toJSON()).toMatchSnapshot(); */
  });

  it('renders correctly for non-sender', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => Lao.fromState(laoState));
    const obj = render(
      <ChirpCard chirp={chirp} />,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('calls delete correctly', () => {
    // TODO: mock the sender
    /* const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => Lao.fromState(laoState));
    const getMockSender = jest.spyOn(KeyPairStore, 'getPublicKey');
    getMockSender.mockImplementation(() => sender);
    const button = render(
      <ChirpCard chirp={chirp} />,
    ).getByLabelText('delete');
    fireEvent.press(button);
    expect(mockRequestDeleteChirp).toHaveBeenCalledTimes(1); */
  });
});

afterAll(() => {
  jest.useRealTimers();
});
