import { useNavigation } from '@react-navigation/core';
import { StackNavigationProp } from '@react-navigation/stack';
import React from 'react';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { Color, Icon } from 'core/styles';

import PoPIcon from './PoPIcon';
import PoPTouchableOpacity from './PoPTouchableOpacity';

type NavigationProps = StackNavigationProp<AppParamList>;

const BackButton = () => {
  const navigation = useNavigation<NavigationProps>();

  return (
    <PoPTouchableOpacity onPress={navigation.goBack}>
      <PoPIcon name="arrowBack" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  );
};

export default BackButton;
