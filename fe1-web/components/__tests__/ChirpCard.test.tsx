import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import {
  Hash, LaoState, PublicKey, Timestamp, Lao, Chirp,
} from 'model/objects';
import {
  requestAddReaction as mockRequestAddReaction,
  requestDeleteChirp as mockRequestDeleteChirp,
} from 'network/MessageApi';
import { OpenedLaoStore } from 'store';
import STRINGS from 'res/strings';
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
const ID = new Hash('1234');
const chirp = new Chirp({
  id: ID,
  text: 'Don\'t panic.',
  sender: sender,
  time: new Timestamp(1609455600), // 31 December 2020
  isDeleted: false,
});

const deletedChirp = new Chirp({
  id: new Hash('1234'),
  text: '',
  sender: sender,
  time: new Timestamp(1609455600), // 31 December 2020
  isDeleted: true,
});

const chirp1 = new Chirp({
  id: new Hash('5678'),
  text: 'Ignore me',
  sender: new PublicKey('Anonymous'),
  time: new Timestamp(1609455600), // 31 December 2020
});

jest.mock('network/MessageApi');
jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn().mockImplementation(() => laoState),
}));

jest.mock('react-redux', () => ({
  useSelector: () => ({ 1234: { '👍': 1, '👎': 0, '❤️': 0 } }),
}));
jest.mock('components/ProfileIcon.tsx', () => () => 'ProfileIcon');

beforeAll(() => {
  jest.useFakeTimers('modern');
  jest.setSystemTime(new Date(1620255600000)); // 5 May 2021
});

describe('ChirpCard', () => {
  describe('for deletion', () => {
    it('renders correctly for sender', () => {
      const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
      getMockLao.mockImplementation(() => Lao.fromState(laoState));
      const obj = render(
        <ChirpCard chirp={chirp} currentUserPublicKey={sender} />,
      );
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('renders correctly for non-sender', () => {
      const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
      getMockLao.mockImplementation(() => Lao.fromState(laoState));
      const obj = render(
        <ChirpCard chirp={chirp} currentUserPublicKey={new PublicKey('IAmNotTheSender')} />,
      );
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('calls delete correctly', () => {
      const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
      getMockLao.mockImplementation(() => Lao.fromState(laoState));
      const { getByLabelText, getByText } = render(
        <ChirpCard chirp={chirp} currentUserPublicKey={sender} />,
      );
      fireEvent.press(getByLabelText('deleteChirpButton'));
      fireEvent.press(getByText(STRINGS.general_yes));
      expect(mockRequestDeleteChirp).toHaveBeenCalledTimes(1);
    });

    it('render correct for a deleted chirp', () => {
      const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
      getMockLao.mockImplementation(() => Lao.fromState(laoState));
      const obj = render(
        <ChirpCard chirp={deletedChirp} currentUserPublicKey={sender} />,
      );
      expect(obj.toJSON()).toMatchSnapshot();
    });
  });

  describe('for reaction', () => {
    it('renders correctly with reaction', () => {
      const obj = render(<ChirpCard chirp={chirp} currentUserPublicKey={sender} />);
      expect(obj.toJSON())
        .toMatchSnapshot();
    });

    it('renders correctly without reaction', () => {
      const obj = render(<ChirpCard chirp={chirp1} currentUserPublicKey={sender} />);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('adds thumbs up correctly', () => {
      const { getByTestId } = render(<ChirpCard chirp={chirp} currentUserPublicKey={sender} />);
      const thumbsUpButton = getByTestId('thumbs-up');
      fireEvent.press(thumbsUpButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('👍', ID);
    });

    it('adds thumbs down correctly', () => {
      const { getByTestId } = render(<ChirpCard chirp={chirp} currentUserPublicKey={sender} />);
      const thumbsDownButton = getByTestId('thumbs-down');
      fireEvent.press(thumbsDownButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('👎', ID);
    });

    it('adds heart correctly', () => {
      const { getByTestId } = render(<ChirpCard chirp={chirp} currentUserPublicKey={sender} />);
      const heartButton = getByTestId('heart');
      fireEvent.press(heartButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('❤️', ID);
    });
  });
});

afterAll(() => {
  jest.useRealTimers();
});
