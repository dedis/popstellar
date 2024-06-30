import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId, mockLaoServerAddress } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER,
  LinkedOrganizationsReactContext,
} from 'features/linked-organizations/interface';
import { linkedOrganizationsReduce } from 'features/linked-organizations/reducer/LinkedOrganizationsReducer';

import AddLinkedOrganizationButton from '../AddLinkedOrganizationButton';

const mockStore = configureStore({
  reducer: combineReducers({
    ...linkedOrganizationsReduce,
  }),
});

const contextValue = {
  [LINKED_ORGANIZATIONS_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useIsLaoOrganizer: () => false,
    useCurrentLao: () => mockLaoServerAddress,
    getLaoById: () => undefined,
    getRollCallById: () => undefined,
  } as LinkedOrganizationsReactContext,
};

describe('AddLinkedOrganizationButton', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={AddLinkedOrganizationButton} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
