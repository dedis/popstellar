import { render } from '@testing-library/react-native';
import React from 'react';

import AutocompleteInput from '../AutocompleteInput';

describe('AutocompleteInput', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <AutocompleteInput value="" onChange={() => {}} suggestions={['aaa', 'aab', 'aac']} />,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
