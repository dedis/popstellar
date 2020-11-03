import React from 'react';
import {
  StyleSheet, View, Text, Button, TextInput,
} from 'react-native';
import PropTypes from 'prop-types';

import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';

/*
* The Launch component
*
* Manage the Launch screen
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-around',
  },
  text: {
    ...Typography.base,
  },
  textInput: {
    ...Typography.base,
    borderBottomWidth: 2,
  },
  button: {
    paddingHorizontal: Spacing.xl,
    paddingVertical: Spacing.s,
  },
  viewBottom: {
    justifyContent: 'flex-end',
  },
});

const cancelAction = (navigation, LAOName) => {
  LAOName.current.clear();
  navigation.navigate('Home');
};

cancelAction.propTypes = {
  navigation: PropTypes.shape({
    navigate: PropTypes.func.isRequired,
  }).isRequired,
};

const Launch = ({ navigation }) => {
  const LAOName = React.useRef();

  return (
    <View style={styles.container}>
      <View style={styles.viewTop}>
        <Text style={styles.text}>{STRINGS.launch_description}</Text>
        <View style={styles.button}>
          <TextInput
            ref={LAOName}
            style={styles.textInput}
            placeholder={STRINGS.launch_organization_name}
          />
        </View>
      </View>
      <View style={styles.viewBottom}>
        <View style={styles.button}>
          <Button title={STRINGS.launch_button_launch} />
        </View>
        <View style={styles.button}>
          <Button
            title={STRINGS.general_button_cancel}
            onPress={() => cancelAction(navigation, LAOName)}
          />
        </View>
      </View>
    </View>
  );
};

Launch.propTypes = {
  navigation: PropTypes.shape({
    navigate: PropTypes.func.isRequired,
  }).isRequired,
};

export default Launch;
