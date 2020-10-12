import React from 'react'
import { StyleSheet, View, Text } from 'react-native'

import STRINGS from '../res/strings'

/*
* The organization component
*/
class Organization extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text>{STRINGS.organization_description}</Text>
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

export default Organization