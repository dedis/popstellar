import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { MeetingReactContext, MEETING_FEATURE_IDENTIFIER } from 'features/meeting/interface';

import CreateMeeting from '../CreateMeeting';

const contextValue = {
  [MEETING_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
  } as MeetingReactContext,
};

describe('CreateMeeting', () => {
  it('renders correctly', () => {
    const component = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateMeeting} />
      </FeatureContext.Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
