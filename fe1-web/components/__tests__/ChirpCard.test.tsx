import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import {
  Hash, LaoState, PublicKey, Timestamp, Lao,
} from 'model/objects';
import { Chirp } from 'model/objects/Chirp';
import { requestDeleteChirp as mockRequestDeleteChirp } from 'network/MessageApi';
import { KeyPairStore, OpenedLaoStore } from 'store';
import ChirpCard from '../ChirpCard';

jest.mock('network/MessageApi');

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
  time: new Timestamp(1609455600),
  likes: 42,
  isDeleted: false,
});

describe('ChirpCard', () => {
  it('renders correctly for sender', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => Lao.fromState(laoState));
    const getMockSender = jest.spyOn(KeyPairStore, 'getPublicKey');
    getMockSender.mockImplementation(() => sender);
    const obj = render(
      <ChirpCard chirp={chirp} />,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('renders correctly for non-sender', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => Lao.fromState(laoState));
    const getMockSender = jest.spyOn(KeyPairStore, 'getPublicKey');
    getMockSender.mockImplementation(() => new PublicKey('Marvin'));
    const obj = render(
      <ChirpCard chirp={chirp} />,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('calls delete correctly', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => Lao.fromState(laoState));
    const getMockSender = jest.spyOn(KeyPairStore, 'getPublicKey');
    getMockSender.mockImplementation(() => sender);
    const button = render(
      <ChirpCard chirp={chirp} />,
    ).getByLabelText('delete');
    fireEvent.press(button);
    expect(mockRequestDeleteChirp).toHaveBeenCalledTimes(1);
  });
});
