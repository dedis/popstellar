import { render } from '@testing-library/react-native';
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
  it('renders correctly', () => {
    const component = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateRollCall} />
      </FeatureContext.Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
