import React from 'react'
import {StyleSheet, Image, View, TouchableOpacity} from 'react-native'

import { Colors } from '../Styles'

/**
* Camera button
* use action parameter to define the onPress action
*/

class CameraButton extends React.Component {
    render() {
        return(
            <View>
                <TouchableOpacity style={styles.button} onPress={ () => { this.props.action() } }>
                    <Image style={styles.icon} source={require("../res/img/ic_camera.png")}/>
                </TouchableOpacity>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    icon:{
        width: 64,
        height: 64,
    },
    button: {
        justifyContent: "center",
        alignItems: "center",
        width: 80,
        height: 80,
        backgroundColor: Colors.blue,
        borderRadius: 80,
    }
  });

export default CameraButton