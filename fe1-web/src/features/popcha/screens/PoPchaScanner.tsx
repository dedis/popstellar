import React from 'react';
import { Text, View } from 'react-native';

import ScreenWrapper from '../../../core/components/ScreenWrapper';
import { Typography } from '../../../core/styles';
import STRINGS from '../../../resources/strings';
import { PoPchaFeature } from '../interface';

const PoPchaScanner = () => {
  return (
    <ScreenWrapper>
      <View>
        <Text style={Typography.paragraph}>Hello World!</Text>
      </View>
    </ScreenWrapper>
  );
};

export default PoPchaScanner;

export const popchaScannerScreen: PoPchaFeature.LaoScreen = {
  id: STRINGS.navigation_lao_popcha,
  Component: PoPchaScanner,
  order: 100000,
};
