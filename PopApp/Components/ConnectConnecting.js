import React from 'react'
import {StyleSheet, View, Text, Button, ActivityIndicator} from 'react-native'

import STRINGS from '../res/strings'
import { Buttons, Colors, Typography } from '../Styles';

/**
 *  Connect to a LAO
 *
 *  Currently, just simulate waiting for a response
*/

class ConnectConnecting extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <View>
                    <Text style={styles.text}>{STRINGS.connect_connecting_uri}</Text>
                </View>
                <View>
                    <View>
                        <ActivityIndicator size="large" color={ Colors.blue }/>
                    </View>
                </View>
                <View>
                    <View style={styles.button} >
                        <Button title={STRINGS.general_button_cancel}
                                onPress={() => {
                                    this.props.navigation.navigate('Scanning');
                                }}/>
                    </View>
                    <View style={styles.button} >
                        <Button title={STRINGS.connect_connecting_validate}
                                onPress={() => {
                                    this.props.navigation.navigate('Confirm');
                                }}/>
                    </View>
                </View>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'space-evenly'
    },
    text: {
        ...Typography.base,
    },
    button:{
        ...Buttons.base
    },
  });

export default ConnectConnecting