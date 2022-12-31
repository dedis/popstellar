import { useNavigation } from '@react-navigation/core';
import { DrawerNavigationProp } from '@react-navigation/drawer';
import PropTypes from 'prop-types';
import React from 'react';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { Color, Icon } from 'core/styles';

import NavigationPadding from './NavigationPadding';
import PoPIcon from './PoPIcon';
import PoPTouchableOpacity from './PoPTouchableOpacity';

type NavigationProps = DrawerNavigationProp<AppParamList>;

const DrawerMenuButton = ({ padding }: IPropTypes) => {
  const navigation = useNavigation<NavigationProps>();

  return (
    <>
      <PoPTouchableOpacity onPress={navigation.toggleDrawer}>
        <PoPIcon name="drawerMenu" color={Color.inactive} size={Icon.size} />
      </PoPTouchableOpacity>
      <NavigationPadding paddingAmount={padding || 0} nextToIcon />
    </>
  );
};

const propTypes = {
  padding: PropTypes.number,
};

DrawerMenuButton.propTypes = propTypes;

DrawerMenuButton.defaultProps = {
  padding: 0,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default DrawerMenuButton;
