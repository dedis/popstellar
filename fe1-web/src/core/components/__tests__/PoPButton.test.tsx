import { render } from '@testing-library/react-native';
import React from 'react';
import { Text } from 'react-native';

import PoPButton from '../PoPButton';

let onPress: Function;
const buttonText = 'I am a wide button';

beforeEach(() => {
  onPress = jest.fn();
});

describe('PoPButton', () => {
  it('renders correctly without disabled', () => {
    const { toJSON } = render(
      <PoPButton onPress={() => onPress}>
        <Text>{buttonText}</Text>
      </PoPButton>,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
