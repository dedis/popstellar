import { render } from '@testing-library/react-native';
import React from 'react';

import PoPTextButton from '../PoPTextButton';

let onPress: Function;
const buttonText = 'I am a wide button';

beforeEach(() => {
  onPress = jest.fn();
});

describe('PoPButton', () => {
  it('renders correctly without disabled', () => {
    const { toJSON } = render(<PoPTextButton onPress={() => onPress}>{buttonText}</PoPTextButton>);
    expect(toJSON()).toMatchSnapshot();
  });
});
