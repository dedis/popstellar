import { render } from '@testing-library/react-native';
import React from 'react';

import Button from '../Button';

let onPress: Function;
const wideButtonTitle = 'I am a wide button';

beforeEach(() => {
  onPress = jest.fn();
});

describe('WideButtonView', () => {
  it('renders correctly without disabled', () => {
    const { toJSON } = render(<Button title={wideButtonTitle} onPress={() => onPress} />);
    expect(toJSON()).toMatchSnapshot();
  });
});
