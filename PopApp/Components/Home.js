import React from 'react'
import { StyleSheet, View, Text } from 'react-native'

import STRINGS from '../res/strings'

/*
* The home component
*/
class Home extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>{STRINGS.home_welcome}</Text>
                <Text style={styles.text}>{STRINGS.home_connect_lao}</Text>
                <Text style={styles.text}>{STRINGS.home_launch_lao}</Text>
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
        textAlign: 'center',
        fontSize: 25,
        fontWeight: 'bold'
    },
  });

export default Home