import { useNavigation } from '@react-navigation/core';
import { StackNavigationProp } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React from 'react';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { Color, Icon } from 'core/styles';

import NavigationPadding from './NavigationPadding';
import PoPIcon from './PoPIcon';
import PoPTouchableOpacity from './PoPTouchableOpacity';

type NavigationProps = StackNavigationProp<AppParamList>;

const BackButton = ({ padding }: IPropTypes) => {
  const navigation = useNavigation<NavigationProps>();

  return (
    <>
      <PoPTouchableOpacity onPress={navigation.goBack}>
        <PoPIcon name="arrowBack" color={Color.inactive} size={Icon.size} />
      </PoPTouchableOpacity>
      <NavigationPadding paddingAmount={padding || 0} nextToIcon />
    </>
  );
};

const propTypes = {
  padding: PropTypes.number,
};

BackButton.propTypes = propTypes;

BackButton.defaultProps = {
  padding: 0,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default BackButton;
