import { render } from '@testing-library/react-native';
import React from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import { mockLaoId } from '__tests__/utils';
import { POPCHA_FEATURE_IDENTIFIER, PoPchaReactContext } from '../../interface';
import PoPchaScanner from '../PoPchaScanner';

const contextValue = {
  [POPCHA_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
  } as PoPchaReactContext,
};

describe('PoPcha scanner', () => {
  it('renders correctly', () => {
    const component = render(
      <FeatureContext.Provider value={contextValue}>
        <PoPchaScanner />
      </FeatureContext.Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
