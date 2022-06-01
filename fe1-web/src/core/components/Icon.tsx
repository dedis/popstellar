import Ionicons from '@expo/vector-icons/Ionicons';
import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View } from 'react-native';

import { ExtendType } from 'core/types';

type IoniconNames = keyof typeof Ionicons['glyphMap'];

type Ionicon = {
  iconName: IoniconNames;
  IconFamily: typeof Ionicons;
};

const iconNameMap = {
  'camera-reverse': {
    iconName: 'ios-camera-reverse',
    IconFamily: Ionicons,
  } as Ionicon,
  close: {
    iconName: 'ios-close',
    IconFamily: Ionicons,
  } as Ionicon,
  code: {
    iconName: 'ios-code',
    IconFamily: Ionicons,
  } as Ionicon,
  copy: {
    iconName: 'ios-copy',
    IconFamily: Ionicons,
  } as Ionicon,
  create: {
    iconName: 'ios-create',
    IconFamily: Ionicons,
  } as Ionicon,
  event: {
    iconName: 'ios-calendar',
    IconFamily: Ionicons,
  } as Ionicon,
  home: {
    iconName: 'ios-home',
    IconFamily: Ionicons,
  } as Ionicon,
  identity: {
    iconName: 'ios-person',
    IconFamily: Ionicons,
  } as Ionicon,
  list: {
    iconName: 'ios-list',
    IconFamily: Ionicons,
  } as Ionicon,
  notification: {
    iconName: 'ios-notifications',
    IconFamily: Ionicons,
  } as Ionicon,
  options: {
    iconName: 'ios-ellipsis-horizontal',
    IconFamily: Ionicons,
  } as Ionicon,
  scan: {
    iconName: 'ios-scan',
    IconFamily: Ionicons,
  } as Ionicon,
  settings: {
    iconName: 'ios-cog',
    IconFamily: Ionicons,
  } as Ionicon,
  socialMedia: {
    iconName: 'ios-people',
    IconFamily: Ionicons,
  } as Ionicon,
  wallet: {
    iconName: 'ios-wallet',
    IconFamily: Ionicons,
  } as Ionicon,
};

const styles = StyleSheet.create({
  focused: {},
});

const Icon = ({ name, color, size, focused }: IconPropTypes) => {
  const Entry = iconNameMap[name];

  if (!Entry) {
    throw new Error(`Unkown icon name ${name}`);
  }

  return (
    <View style={focused ? styles.focused : undefined}>
      <Entry.IconFamily name={Entry.iconName} size={size} color={color} />
    </View>
  );
};

const iconPropTypes = {
  name: PropTypes.string.isRequired,
  color: PropTypes.string.isRequired,
  size: PropTypes.number.isRequired,
  focused: PropTypes.bool,
};

Icon.propTypes = iconPropTypes;
Icon.defaultProps = {
  focused: false,
};

type IconPropTypes = ExtendType<
  PropTypes.InferProps<typeof iconPropTypes>,
  {
    name: keyof typeof iconNameMap;
  }
>;

export default Icon;

export const makeIcon = (name: keyof typeof iconNameMap) => {
  const Entry = iconNameMap[name];

  if (!Entry) {
    throw new Error(`Unkown icon name ${name}`);
  }

  return ({ color, size, focused }: Omit<IconPropTypes, 'name'>) => (
    <View style={focused ? styles.focused : undefined}>
      <Entry.IconFamily name={Entry.iconName} size={size} color={color} />
    </View>
  );
};
