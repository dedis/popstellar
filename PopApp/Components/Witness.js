import React from 'react'
import { StyleSheet, View, Text } from 'react-native'

import STRINGS from "../res/strings";
import { Typography } from '../Styles'


/**
* The Witness component
*
* Manage the Witness screen
*/
class Witness extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>{STRINGS.witness_description}</Text>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center'
    },
    text: {
        ...Typography.base
    },
  });

export default Witness