import React from 'react';
import { render } from '@testing-library/react-native';
import { Chirp } from 'model/objects/Chirp';
import { Hash, PublicKey, Timestamp } from 'model/objects';
import DeletedChirpCard from '../DeletedChirpCard';

const chirp = new Chirp({
  id: new Hash('1234'),
  text: 'Don\'t panic.',
  sender: new PublicKey('Douglas Adams'),
  time: new Timestamp(1609455600),
  likes: 42,
  isDeleted: true,
});

describe('DeletedChirpCard', () => {
  it('renders correctly', () => {
    const obj = render(
      <DeletedChirpCard chirp={chirp} />,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });
});
