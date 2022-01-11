import React from 'react';
import { render } from '@testing-library/react-native';
import { Hash, PublicKey, Timestamp } from 'model/objects';
import { Chirp } from 'model/objects/Chirp';
import ChirpCard from '../ChirpCard';

const chirp = new Chirp({
  id: new Hash('1234'),
  text: 'Don\'t panic.',
  sender: new PublicKey('Douglas Adams'),
  time: new Timestamp(1609455600), // 31 December 2020
  likes: 42,
});

jest.mock('components/ProfileIcon.tsx', () => () => 'ProfileIcon');

beforeAll(() => {
  jest.useFakeTimers('modern');
  jest.setSystemTime(new Date(1620255600000)); // 5 May 2021
});

describe('ChirpCard', () => {
  it('renders correctly', () => {
    const obj = render(
      <ChirpCard
        chirp={chirp}
      />,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });
});

afterAll(() => {
  jest.useRealTimers();
});
