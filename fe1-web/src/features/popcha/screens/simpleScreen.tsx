import React from 'react';
import { Text, View } from 'react-native';

import ScreenWrapper from '../../../core/components/ScreenWrapper';
import { Typography } from '../../../core/styles';
import { PoPchaFeature } from '../interface';

const simpleScreen = () => {
  return (
    <ScreenWrapper>
      <View>
        <Text style={Typography.paragraph}>Hello World</Text>
      </View>
    </ScreenWrapper>
  );
};

export default simpleScreen;

export const simpleScreenScreen: PoPchaFeature.LaoScreen = {
  id: 'simpleScreen',
  Component: simpleScreen,
};
