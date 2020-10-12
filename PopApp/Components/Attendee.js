import React from 'react'
import { StyleSheet, View, Text } from 'react-native'

import STRINGS from '../res/strings'

/*
* The attendee component
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
        justifyContent: 'center',
        alignContent: 'center',
        alignItems: 'center'
    },
    text: {
    },
  });

export default Attendee