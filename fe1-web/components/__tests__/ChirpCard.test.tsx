import React from 'react';
import { fireEvent, render } from '@testing-library/react-native';
import { Hash, PublicKey, Timestamp } from 'model/objects';
import { Chirp } from 'model/objects/Chirp';
import { requestAddReaction as mockRequestAddReaction } from 'network/MessageApi';
import ChirpCard from '../ChirpCard';

const ID = new Hash('1234');
const chirp = new Chirp({
  id: ID,
  text: 'Don\'t panic.',
  sender: new PublicKey('Douglas Adams'),
  time: new Timestamp(1609455600), // 31 December 2020
});

const chirp1 = new Chirp({
  id: new Hash('5678'),
  text: 'Ignore me',
  sender: new PublicKey('Anonymous'),
  time: new Timestamp(1609455600), // 31 December 2020
});

jest.mock('network/MessageApi');
jest.mock('react-redux', () => ({
  useSelector: () => ({ 1234: { '👍': 1, '👎': 0, '❤️': 0 } }),
}));
beforeAll(() => {
  jest.useFakeTimers('modern');
  jest.setSystemTime(new Date(1620255600000)); // 5 May 2021
});

describe('ChirpCard', () => {
  it('renders correctly with reaction', () => {
    const obj = render(<ChirpCard chirp={chirp} />);
    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('renders correctly without reaction', () => {
    const obj = render(<ChirpCard chirp={chirp1} />);
    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('adds thumbs up correctly', () => {
    const { getByTestId } = render(<ChirpCard chirp={chirp} />);
    const thumbsUpButton = getByTestId('thumbs-up');
    fireEvent.press(thumbsUpButton);
    expect(mockRequestAddReaction).toHaveBeenCalledWith('👍', ID);
  });

  it('adds thumbs down correctly', () => {
    const { getByTestId } = render(<ChirpCard chirp={chirp} />);
    const thumbsDownButton = getByTestId('thumbs-down');
    fireEvent.press(thumbsDownButton);
    expect(mockRequestAddReaction).toHaveBeenCalledWith('👎', ID);
  });

  it('adds heart correctly', () => {
    const { getByTestId } = render(<ChirpCard chirp={chirp} />);
    const heartButton = getByTestId('heart');
    fireEvent.press(heartButton);
    expect(mockRequestAddReaction).toHaveBeenCalledWith('❤️', ID);
  });
});

afterAll(() => {
  jest.useRealTimers();
});
