import { Ionicons, MaterialCommunityIcons, MaterialIcons } from '@expo/vector-icons';
import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View } from 'react-native';

import { Color, Icon } from 'core/styles';
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
type IonIcon = {
  iconName: IoniconNames;
  IconFamily: typeof Ionicons;
};

type MaterialIconNames = keyof typeof MaterialIcons['glyphMap'];

type MaterialIcon = {
  iconName: MaterialIconNames;
  IconFamily: typeof MaterialIcons;
};

type MaterialCommunityIconNames = keyof typeof MaterialCommunityIcons['glyphMap'];

type MaterialCommunityIcon = {
  iconName: MaterialCommunityIconNames;
  IconFamily: typeof MaterialCommunityIcons;
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
  addPerson: {
    iconName: 'ios-person-add',
    IconFamily: Ionicons,
    /**
     * the 'as' typecast will make the linter yell at you if you tried to enter an invalid
     * combination of name and family
     */
  } as IonIcon,
  digitalCash: {
    iconName: 'attach-money',
    IconFamily: MaterialIcons,
  } as MaterialIcon,
  cameraReverse: {
    iconName: 'ios-camera-reverse',
    IconFamily: Ionicons,
  } as IonIcon,
  close: {
    iconName: 'ios-close',
    IconFamily: Ionicons,
  } as IonIcon,
  code: {
    iconName: 'ios-code',
    IconFamily: Ionicons,
  } as IonIcon,
  copy: {
    iconName: 'ios-copy',
    IconFamily: Ionicons,
  } as IonIcon,
  create: {
    iconName: 'ios-create',
    IconFamily: Ionicons,
  } as IonIcon,
  delete: {
    iconName: 'ios-trash',
    IconFamily: Ionicons,
  } as IonIcon,
  dropdown: {
    iconName: 'ios-chevron-down',
    IconFamily: Ionicons,
  } as IonIcon,
  election: {
    iconName: 'how-to-vote',
    IconFamily: MaterialIcons,
  } as MaterialIcon,
  event: {
    iconName: 'ios-calendar',
    IconFamily: Ionicons,
  } as IonIcon,
  home: {
    iconName: 'ios-home',
    IconFamily: Ionicons,
  } as IonIcon,
  identity: {
    iconName: 'ios-person',
    IconFamily: Ionicons,
  } as IonIcon,
  info: {
    iconName: 'ios-information-circle-outline',
    IconFamily: Ionicons,
  } as IonIcon,
  list: {
    iconName: 'ios-list',
    IconFamily: Ionicons,
  } as IonIcon,
  meeting: {
    iconName: 'ios-calendar',
    IconFamily: Ionicons,
  } as IonIcon,
  notification: {
    iconName: 'ios-notifications',
    IconFamily: Ionicons,
  } as IonIcon,
  options: {
    iconName: 'ios-ellipsis-horizontal',
    IconFamily: Ionicons,
  } as IonIcon,
  qrCode: {
    iconName: 'ios-qr-code',
    IconFamily: Ionicons,
  } as IonIcon,
  rollCall: {
    iconName: 'ios-hand-left',
    IconFamily: Ionicons,
  } as IonIcon,
  scan: {
    iconName: 'ios-scan',
    IconFamily: Ionicons,
  } as IonIcon,
  scanQrCode: {
    iconName: 'qrcode-scan',
    IconFamily: MaterialCommunityIcons,
  } as MaterialCommunityIcon,
  settings: {
    iconName: 'ios-cog',
    IconFamily: Ionicons,
  } as IonIcon,
  socialMedia: {
    iconName: 'ios-people',
    IconFamily: Ionicons,
  } as IonIcon,
  wallet: {
    iconName: 'ios-wallet',
    IconFamily: Ionicons,
  } as IonIcon,
  warning: {
    iconName: 'ios-warning',
    IconFamily: Ionicons,
  } as IonIcon,
  witness: {
    iconName: 'ios-eye',
    IconFamily: Ionicons,
  } as IonIcon,
};

const styles = StyleSheet.create({
  focused: {},
});

const PoPIcon = ({ name, color, size, focused }: IconPropTypes) => {
  // we need to cast it here to a more generic type due to limitations
  // in the static type checking
  const Entry = iconNameMap[name] as {
    iconName: string;
    IconFamily: React.ComponentType<{ name: string; size: number; color: string }>;
  };

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
  color: PropTypes.string,
  size: PropTypes.number,
  focused: PropTypes.bool,
};

PoPIcon.propTypes = iconPropTypes;
PoPIcon.defaultProps = {
  focused: false,
  color: Color.primary,
  size: Icon.size,
};

type IconPropTypes = ExtendType<
  PropTypes.InferProps<typeof iconPropTypes>,
  {
    name: keyof typeof iconNameMap;
    color: string;
    size: number;
  }
>;

export default PoPIcon;

export const makeIcon = (name: keyof typeof iconNameMap) => {
  // we need to cast it here to a more generic type due to limitations
  // in the static type checking
  const Entry = iconNameMap[name] as {
    iconName: string;
    IconFamily: React.ComponentType<{ name: string; size: number; color: string }>;
  };

  if (!Entry) {
    throw new Error(`Unkown icon name ${name}`);
  }

  return ({ color, size, focused }: Omit<IconPropTypes, 'name'>) => (
    <View style={focused ? styles.focused : undefined}>
      <Entry.IconFamily name={Entry.iconName} size={size} color={color} />
    </View>
  );
};
