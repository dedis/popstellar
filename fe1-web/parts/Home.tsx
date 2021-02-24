import React, { Component } from 'react';
import { StyleSheet, View, FlatList } from 'react-native';
import { connect } from 'react-redux';

import { makeLaosList } from 'store/reducers';

import { Spacing } from 'styles/index';
import styleContainer from 'styles/stylesheets/container';
import STRINGS from 'res/strings';
import LAOItem from 'components/LAOItem';
import TextBlock from 'components/TextBlock';

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

class Home extends Component {
  private getHomeScreen() {
    const { laos } = this.props;
    if (laos !== undefined && !laos.length) {
      return Home.getConnectedLaosDisplay(laos);
    }
    return Home.getWelcomeMessageDisplay();
  }

  private static getConnectedLaosDisplay(laos) {
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

  private static getWelcomeMessageDisplay() {
    return (
      <View style={styleContainer.centered}>
        <TextBlock bold text={STRINGS.home_welcome} />
        <TextBlock bold text={STRINGS.home_connect_lao} />
        <TextBlock bold text={STRINGS.home_launch_lao} />
      </View>
    );
  }

  render() {
    return this.getHomeScreen();
  }
}
/* // FIXME add back when available lao storage is refactored
Home.propTypes = {
  laos: PropTypes.arrayOf(PROPS_TYPE.LAO).isRequired,
}; */

const laosList = makeLaosList();
const mapStateToProps = (state: any) => ({
  laos: laosList(state),
});

export default connect(mapStateToProps)(Home);
