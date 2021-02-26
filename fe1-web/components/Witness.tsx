import React from 'react';
import {
  StyleSheet, View, Button, FlatList,
} from 'react-native';

import { Buttons, Typography } from 'styles';
import STRINGS from 'res/strings';
import PROPS_TYPE, { INavigation } from 'res/Props';
import Attendee from 'parts/lao/attendee/Attendee';

/**
 * Manage the Witness screen: button to navigate to the witness video screen,
 * a section list of events and propreties (see attendee for more details)
*/
const styles = StyleSheet.create({
  container: {
  },
  text: {
    ...Typography.base,
  },
  button: {
    ...Buttons.base,
  },
});

interface IPropTypes {
  navigation: INavigation;
}

const Witness = ({ navigation }: IPropTypes) => (
  <FlatList
    style={styles.container}
    ListHeaderComponent={(
      <View>
        <View style={styles.button}>
          <Button
            onPress={() => navigation.navigate(STRINGS.witness_navigation_tab_video)}
            title={STRINGS.witness_video_button}
          />
        </View>
        <Attendee />
      </View>
    )}
    data={[]}
    renderItem={() => null}
  />
);

Witness.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

export default Witness;
