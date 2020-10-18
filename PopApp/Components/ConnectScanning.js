import React from 'react'
import { StyleSheet, View, Text } from 'react-native'

import STRINGS from '../res/strings'
import { Typography } from '../Styles'
import CameraButton from './CameraButton'


/**
* Scanning connect component
*
* In the future will scan a QR code and connect to the LAO, not just a dummy button
*/

class ConnectScanning extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>{STRINGS.connect_scanning_camera_view}</Text>
                <CameraButton action= {() => {this.props.navigation.navigate("Connecting");}} />
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'space-evenly'
    },
    text: {
        ...Typography.base
    },
  });

export default ConnectScanning