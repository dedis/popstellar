import Ionicons from '@expo/vector-icons/Ionicons';
import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View } from 'react-native';

import { ExtendType } from 'core/types';

/**
 * A union type for all valid Ionicon icon names
 */
type IoniconNames = keyof typeof Ionicons['glyphMap'];

/**
 * This tuple uniquely determines a IonIcon icon.
 * @expo/vector-icons provides a set of icon families, each of them
 * has a set of icons that each has a name. This type makes sure
 * that the linter realizes when a invalid combination of
 * iconName and IconFamily is used.
 */
type Ionicon = {
  iconName: IoniconNames;
  IconFamily: typeof Ionicons;
};

/**
 * To add icons from new icon families, create an Icon type similar to the one above
 * that contains the two fields 'iconName' and 'IconFamily' where the type of
 * IconFamily should be a @expo/vector-icons icon family component and
 * iconName should be the union type of all valid icon names for the respective family
 */

/**
 * This is a map from our custom icon names to a tuple that uniquely
 * determines an icon (it contains the family and the name inside the family).
 * This map allows the component to look up what icon family component should be
 * used and what name should be passed to it.
 */
const iconNameMap = {
  'camera-reverse': {
    iconName: 'ios-camera-reverse',
    IconFamily: Ionicons,
    /**
     * the 'as' typecast will make the linter yell at you if you tried to enter an invalid
     * combination of name and family
     */
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
