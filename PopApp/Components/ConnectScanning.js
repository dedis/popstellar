import React from 'react'
import {StyleSheet, View, Text, Image} from 'react-native'
import { TouchableOpacity } from 'react-native-gesture-handler'

import STRINGS from '../res/strings'

/*
* Scanning connect component
*
* In the future will scan a QR code and connect to the LAO, not just a dummy button
*/

class ConnectScanning extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>{STRINGS.connect_scanning_camera_view}</Text>
                <TouchableOpacity style={styles.button} onPress={ () => {this.props.navigation.navigate("Connecting");} }>
                    <Image style={styles.icon} source={require("../res/img/ic_camera.png")}/>
                </TouchableOpacity>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: "space-evenly",
        alignItems: "center",
    },
    text: {
        fontSize: 25,
        textAlign: "center",
        marginHorizontal: 10
    },
    icon:{
        width: 64,
        height: 64,
    },
    button: {
        justifyContent: "center",
        alignItems: "center",
        width: 80,
        height: 80,
        backgroundColor: '#2196F3',
        borderRadius: 80,
    }
  });

export default ConnectScanning