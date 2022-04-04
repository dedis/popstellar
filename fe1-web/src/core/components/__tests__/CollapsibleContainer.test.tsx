import { describe } from '@jest/globals';
import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';
import { Text } from 'react-native';

import CollapsibleContainer from '../CollapsibleContainer';

describe('CollapsibleContainer', () => {
  it('renders correctly when closed', () => {
    const component = render(
      <CollapsibleContainer title="A collapsible container">
        <Text>a first child</Text>
        <Text>a second child</Text>
      </CollapsibleContainer>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly when opened', () => {
    const component = render(
      <CollapsibleContainer title="A collapsible container" isInitiallyOpen>
        <Text>a first child</Text>
        <Text>a second child</Text>
      </CollapsibleContainer>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('container can be opened', async () => {
    const { getByText, toJSON } = render(
      <CollapsibleContainer title="open me">
        <Text>a first child</Text>
        <Text>a second child</Text>
      </CollapsibleContainer>,
    );

    fireEvent.press(getByText('open me'));

    expect(toJSON()).toMatchSnapshot();
  });
});
