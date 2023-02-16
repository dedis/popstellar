import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { makeEventByTypeSelector } from 'features/events/reducer';
import { RollCallReactContext, ROLLCALL_FEATURE_IDENTIFIER } from 'features/rollCall/interface';

import CreateRollCall from '../CreateRollCall';

const contextValue = {
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
    makeEventByTypeSelector,
    generateToken: jest.fn(),
    hasSeed: jest.fn(),
  } as RollCallReactContext,
};

describe('CreateRollCall', () => {
  it('renders correctly when name is empty', () => {
    const { getByTestId, toJSON } = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateRollCall} />
      </FeatureContext.Provider>,
    );

    const locationInput = getByTestId('roll_call_location_selector');
    fireEvent.changeText(locationInput, 'EPFL');
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when location is empty', () => {
    const { getByTestId, toJSON } = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateRollCall} />
      </FeatureContext.Provider>,
    );

    const nameInput = getByTestId('roll_call_name_selector');
    fireEvent.changeText(nameInput, 'myRollCall');
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when location and name are not empty', () => {
    const { getByTestId, toJSON } = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateRollCall} />
      </FeatureContext.Provider>,
    );

    const nameInput = getByTestId('roll_call_name_selector');
    const locationInput = getByTestId('roll_call_location_selector');
    fireEvent.changeText(nameInput, 'myRollCall');
    fireEvent.changeText(locationInput, 'EPFL');
    expect(toJSON()).toMatchSnapshot();
  });
});
