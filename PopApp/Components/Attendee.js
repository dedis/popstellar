import React from 'react'
import { StyleSheet, View, Text } from 'react-native'

import STRINGS from "../res/strings";
import { Views } from '../Styles';


/**
* The Attendee component
*
* Manage the Attendee screen
*/
class Attendee extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text>{STRINGS.attendee_description}</Text>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        ...Views.base,
    },
  });

export default Attendee