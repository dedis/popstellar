import React from 'react';
import { StyleSheet, View, FlatList } from 'react-native';
import { connect } from 'react-redux';

import { Spacing } from 'styles/index';
import styleContainer from 'styles/stylesheets/container';
import STRINGS from 'res/strings';
import LAOItem from 'components/LAOItem';
import TextBlock from 'components/TextBlock';
import PROPS_TYPE from 'res/Props';
import PropTypes from 'prop-types';

/**
 * Manage the Home screen component: if the user is not connected to any LAO, a welcome message
 * is displayed, otherwise a list available previously connected LAOs is displayed instead
 *
 * TODO use the list that the user have already connect to, and ask data to
 *  some organizer server if needed
*/
const styles = StyleSheet.create({
  flatList: {
    marginTop: Spacing.s,
  },
});

// FIXME: define interface + types, requires availableLaosReducer to be migrated first
function getConnectedLaosDisplay(laos) {
  return (
    <View style={styleContainer.centered}>
      <FlatList
        data={laos}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <LAOItem LAO={item} />}
        style={styles.flatList}
      />
    </View>
  );
}

function getWelcomeMessageDisplay() {
  return (
    <View style={styleContainer.centered}>
      <TextBlock bold text={STRINGS.home_welcome} />
      <TextBlock bold text={STRINGS.home_connect_lao} />
      <TextBlock bold text={STRINGS.home_launch_lao} />
    </View>
  );
}

const Home = (props: IPropTypes) => {
  const { laos } = props;

  return (laos && !laos.length)
    ? getConnectedLaosDisplay(laos)
    : getWelcomeMessageDisplay();
};

const propTypes = {
  laos: PropTypes.arrayOf(PROPS_TYPE.LAO),
};
Home.propTypes = propTypes;

Home.defaultProps = {
  laos: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

const mapStateToProps = (state: any) => ({
  laos: state.availableLaos.LAOs,
});

export default connect(mapStateToProps)(Home);
