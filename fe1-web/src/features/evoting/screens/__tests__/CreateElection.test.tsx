import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoIdHash, messageRegistryInstance, mockReduxAction } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { EVOTING_FEATURE_IDENTIFIER } from 'features/evoting/interface';

import CreateElection from '../CreateElection';

const contextValue = {
  [EVOTING_FEATURE_IDENTIFIER]: {
    getCurrentLao: () => mockLao,
    getCurrentLaoId: () => mockLaoIdHash,
    useCurrentLao: () => mockLao,
    useCurrentLaoId: () => mockLaoIdHash,
    addEvent: () => mockReduxAction,
    updateEvent: () => mockReduxAction,
    getEventFromId: () => undefined,
    messageRegistry: messageRegistryInstance,
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
