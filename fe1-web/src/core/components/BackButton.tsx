import { useNavigation } from '@react-navigation/core';
import { StackNavigationProp } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React from 'react';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { Color, Icon } from 'core/styles';

import ButtonPadding from './ButtonPadding';
import PoPIcon from './PoPIcon';
import PoPTouchableOpacity from './PoPTouchableOpacity';

type NavigationProps = StackNavigationProp<AppParamList>;

const BackButton = ({ padding, testID }: IPropTypes) => {
  const navigation = useNavigation<NavigationProps>();

  return (
    <>
      <PoPTouchableOpacity onPress={navigation.goBack} testID={testID}>
        <PoPIcon name="arrowBack" color={Color.inactive} size={Icon.size} />
      </PoPTouchableOpacity>
      <ButtonPadding paddingAmount={padding || 0} nextToIcon />
    </>
  );
};

const propTypes = {
  padding: PropTypes.number,
  testID: PropTypes.string,
};

BackButton.propTypes = propTypes;

BackButton.defaultProps = {
  padding: 0,
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default BackButton;
