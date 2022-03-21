import { render } from '@testing-library/react-native';
import React from 'react';

import WideButtonView from '../WideButtonView';

let onPress: Function;
const wideButtonTitle = 'I am a wide button';

beforeEach(() => {
  onPress = jest.fn();
});

describe('WideButtonView', () => {
  it('renders correctly without disabled', () => {
    const { toJSON } = render(<WideButtonView title={wideButtonTitle} onPress={() => onPress} />);
    expect(toJSON()).toMatchSnapshot();
  });
});
