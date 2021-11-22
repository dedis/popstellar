import React from 'react';
import { render } from '@testing-library/react-native';
import { Timestamp } from 'model/objects';
import ChirpCard from '../ChirpCard';

const text = 'Don\'t panic.';
const sender = 'Douglas Adams';
const time = new Timestamp(1609455600);

describe('ChirpCard', () => {
  it('renders correctly', () => {
    const obj = render(
      <ChirpCard
        sender={sender}
        text={text}
        time={time}
        likes={42}
      />,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });
});
