import React from 'react';
import { Text, View } from 'react-native';

import { makeIcon } from 'core/components/PoPIcon';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { PoPchaHooks } from '../hooks';
import { PoPchaFeature } from '../interface';

const PoPchaScanner = () => {
  const laoId = PoPchaHooks.useCurrentLaoId();

  return (
    <ScreenWrapper>
      <View>
        <Text style={Typography.paragraph}>Hello, here is your laoID: {laoId}</Text>
      </View>
    </ScreenWrapper>
  );
};

export default PoPchaScanner;

export const popchaScannerScreen: PoPchaFeature.LaoScreen = {
  id: STRINGS.navigation_lao_popcha,
  Icon: makeIcon('scan'),
  Component: PoPchaScanner,
  order: 100000,
};
