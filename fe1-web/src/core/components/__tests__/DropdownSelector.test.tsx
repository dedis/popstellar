import { render } from '@testing-library/react-native';
import React from 'react';

import DropdownSelector from '../DropdownSelector';

describe('DropdownSelector', () => {
  it('renders correctly without preselected value', () => {
    const { toJSON } = render(
      <DropdownSelector
        onChange={() => {}}
        options={[
          { label: 'some label', value: 'some value' },
          { label: 'some other label', value: 'some other value' },
        ]}
      />,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly with preselected value', () => {
    const { toJSON } = render(
      <DropdownSelector
        onChange={() => {}}
        selected="some value"
        options={[
          { label: 'some label', value: 'some value' },
          { label: 'some other label', value: 'some other value' },
        ]}
      />,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correclty with an invalid preselected value', () => {
    const { toJSON } = render(
      <DropdownSelector
        onChange={() => {}}
        selected="some invalid value"
        options={[
          { label: 'some label', value: 'some value' },
          { label: 'some other label', value: 'some other value' },
        ]}
      />,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
