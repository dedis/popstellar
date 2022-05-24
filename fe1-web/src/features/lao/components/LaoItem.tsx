import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, TextStyle, TouchableOpacity, View, ViewStyle } from 'react-native';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { HomeParamList } from 'core/navigation/typing/HomeParamList';
import { Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { Lao } from '../objects';

/**
 * The LAO item component: name of LAO
 *
 * On click go to the organization screen and store the ID of the LAO for the organization screen
 */
const styles = StyleSheet.create({
  view: {
    marginBottom: Spacing.x1,
  } as ViewStyle,
  text: {
    ...Typography.baseCentered,
    borderWidth: 1,
    borderRadius: 5,
  } as TextStyle,
});

type NavigationProps = CompositeScreenProps<
  StackScreenProps<HomeParamList, typeof STRINGS.navigation_home_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_home>
>;

const LaoItem = ({ lao }: IPropTypes) => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const handlePress = () => {
    navigation.push(STRINGS.navigation_app_home, {
      screen: STRINGS.navigation_home_mock_connect,
      params: {
        screen: STRINGS.navigation_connect_confirm,
        params: {
          laoId: lao.id.valueOf(),
          serverUrl: lao.server_addresses[0],
        },
      },
    });
  };

  return (
    <View style={styles.view}>
      <TouchableOpacity onPress={handlePress}>
        <Text style={styles.text}>{lao.name}</Text>
      </TouchableOpacity>
    </View>
  );
};

const propTypes = {
  lao: PropTypes.instanceOf(Lao).isRequired,
};
LaoItem.propTypes = propTypes;
type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default LaoItem;
