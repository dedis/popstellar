import { render } from '@testing-library/react-native';
import React from 'react';

import ScannerInput from '../ScannerInput';

describe('ScannerInput', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <ScannerInput value="" onPress={() => {}} suggestions={['aaa', 'aab', 'aac']} />,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
