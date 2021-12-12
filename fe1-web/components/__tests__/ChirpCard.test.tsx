import React from 'react';
import { render } from '@testing-library/react-native';
import { Hash, PublicKey, Timestamp } from 'model/objects';
import { Chirp } from 'model/objects/Chirp';
import ChirpCard from '../ChirpCard';

const chirp = new Chirp({
  id: new Hash('1234'),
  text: 'Don\'t panic.',
  sender: new PublicKey('Douglas Adams'),
  time: new Timestamp(1609455600),
  likes: 42,
  isDeleted: 0,
});

describe('ChirpCard', () => {
  it('renders correctly', () => {
    const obj = render(
      <ChirpCard chirp={chirp} />,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });
});
