import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';

import { Hash, PublicKey, Timestamp } from 'model/objects';
import { OpenedLaoStore } from 'store';
import STRINGS from 'res/strings';
import { mockLao, mockLaoState } from '__tests__/utils/TestUtils';

import {
  requestAddReaction as mockRequestAddReaction,
  requestDeleteChirp as mockRequestDeleteChirp,
} from '../../network/SocialMessageApi';
import { Chirp } from '../../objects';
import ChirpCard from '../ChirpCard';

let chirp: Chirp;
let chirp1: Chirp;
let deletedChirp: Chirp;
let sender: PublicKey;
let ID: Hash;

const initializeData = () => {
  const TIMESTAMP = 1609455600; // 31 December 2020
  sender = new PublicKey('Douglas Adams');
  ID = new Hash('1234');

  chirp = new Chirp({
    id: ID,
    text: 'Don\'t panic.',
    sender: sender,
    time: new Timestamp(TIMESTAMP),
    isDeleted: false,
  });

  deletedChirp = new Chirp({
    id: new Hash('1234'),
    text: '',
    sender: sender,
    time: new Timestamp(TIMESTAMP),
    isDeleted: true,
  });

  chirp1 = new Chirp({
    id: new Hash('5678'),
    text: 'Ignore me',
    sender: new PublicKey('Anonymous'),
    time: new Timestamp(TIMESTAMP),
  });
};

jest.mock('network/MessageApi');
jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn().mockImplementation(() => mockLaoState),
}));

jest.mock('react-redux', () => ({
  useSelector: () => ({ 1234: { '👍': 1, '👎': 0, '❤️': 0 } }),
}));
jest.mock('components/ProfileIcon.tsx', () => () => 'ProfileIcon');

beforeAll(() => {
  jest.useFakeTimers('modern');
  jest.setSystemTime(new Date(1620255600000)); // 5 May 2021
});

beforeEach(() => {
  initializeData();
});

describe('ChirpCard', () => {
  describe('for deletion', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => mockLao);

    it('renders correctly for sender', () => {
      const obj = render(
        <ChirpCard chirp={chirp} currentUserPublicKey={sender} />,
      );
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('renders correctly for non-sender', () => {
      const obj = render(
        <ChirpCard chirp={chirp} currentUserPublicKey={new PublicKey('IAmNotTheSender')} />,
      );
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('calls delete correctly', () => {
      const { getByLabelText, getByText } = render(
        <ChirpCard chirp={chirp} currentUserPublicKey={sender} />,
      );
      fireEvent.press(getByLabelText('deleteChirpButton'));
      fireEvent.press(getByText(STRINGS.general_yes));
      expect(mockRequestDeleteChirp).toHaveBeenCalledTimes(1);
    });

    it('render correct for a deleted chirp', () => {
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
