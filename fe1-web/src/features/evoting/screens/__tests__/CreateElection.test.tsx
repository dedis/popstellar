import { render } from '@testing-library/react-native';
import React from 'react';

import { EVOTING_FEATURE_IDENTIFIER } from 'features/evoting';
import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoIdHash, mockMessageRegistry, mockReduxAction } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';

import CreateElection from '../CreateElection';

const contextValue = {
  [EVOTING_FEATURE_IDENTIFIER]: {
    getCurrentLao: () => mockLao,
    getCurrentLaoId: () => mockLaoIdHash,
    addEvent: () => mockReduxAction,
    updateEvent: () => mockReduxAction,
    getEventFromId: () => undefined,
    messageRegistry: mockMessageRegistry,
    onConfirmEventCreation: () => undefined,
  },
};

describe('CreateElection', () => {
  it('renders correctly', () => {
    const component = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateElection} />
      </FeatureContext.Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
