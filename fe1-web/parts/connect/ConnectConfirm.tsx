import React from 'react';
import {
  StyleSheet, View, ScrollView, ViewStyle,
} from 'react-native';

import { Spacing } from 'styles';
import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import PropTypes from 'prop-types';
import styleContainer from 'styles/stylesheets/container';

/**
 * Ask for confirmation to connect to a specific LAO
 * The ScrollView shows information for the user to verify the authenticity of the LAO
 *
 * TODO Make the confirm button make the action require in the UI specification
*/
const styles = StyleSheet.create({
  viewCenter: {
    flex: 8,
    justifyContent: 'center',
    borderWidth: 1,
    margin: Spacing.xs,
  } as ViewStyle,
});

const onButtonConfirm = (navigation: any) => {
  const parentNavigation = navigation.dangerouslyGetParent();
  if (parentNavigation !== undefined) {
    parentNavigation.navigate(STRINGS.navigation_tab_launch);
  }
};

const ConnectConfirm = ({ navigation }: IPropTypes) => (
  <View style={styleContainer.flex}>
    <TextBlock text={STRINGS.connect_confirm_description} />
    <View style={styles.viewCenter}>
      <ScrollView>
        <TextBlock text={STRINGS.lorem_ipsum} />
      </ScrollView>
    </View>
    <WideButtonView
      title={STRINGS.general_button_confirm}
      onPress={() => onButtonConfirm(navigation)}
    />
    <WideButtonView
      title={STRINGS.general_button_cancel}
      onPress={() => navigation.navigate(STRINGS.connect_scanning_title)}
    />
  </View>
);

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
ConnectConfirm.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ConnectConfirm;
