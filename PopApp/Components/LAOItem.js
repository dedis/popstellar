import React from 'react'
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native'
import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';


/*
* The LAO item component
*
* 
*/

class LAOItem extends React.Component {
  _handlePress() {
    let parentNavigation = this.props.navigation.dangerouslyGetParent();
    if(parentNavigation != undefined){
        parentNavigation.navigate(STRINGS.app_navigation_tab_organizer)
    }
  }

  render() {
    const LAO = this.props.LAO
    return(
        <View style={styles.view}>
          <TouchableOpacity onPress={() => this._handlePress()}>
            <Text style={styles.text}>{LAO.name}</Text>
          </TouchableOpacity>
        </View>
    )
  }
}


const styles = StyleSheet.create({
  view:{
    marginBottom: Spacing.xs
  },
  text:{
    ...Typography.base,
    borderWidth: 1,
    borderRadius: 5,
  }
});

export default LAOItem